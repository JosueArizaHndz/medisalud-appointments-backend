package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.model.Penalty;
import com.medisalud.appointments.domain.port.in.*;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import com.medisalud.appointments.domain.port.out.PenaltyRepositoryPort;
import com.medisalud.appointments.domain.port.out.PatientRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService implements AppointmentServiceInterface, AppointmentQueryPort {

    private final AppointmentRepositoryPort appointmentRepositoryPort;
    private final DoctorRepositoryPort doctorRepositoryPort;
    private final PatientRepositoryPort patientRepositoryPort;
    private final PenaltyRepositoryPort penaltyRepositoryPort;

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepositoryPort.findAll();
    }

    @Override
    public Appointment getAppointmentById(UUID id) {
        return appointmentRepositoryPort.findById(id)
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Cita no encontrada con id: " + id));
    }

    @Override
    public List<Appointment> getAppointmentsByDoctorId(UUID doctorId) {
        return appointmentRepositoryPort.findByDoctorId(doctorId);
    }

    @Override
    public List<Appointment> getAppointmentsByPatientId(UUID patientId) {
        return appointmentRepositoryPort.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return appointmentRepositoryPort.findByAppointmentDateBetween(start, end);
    }

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentCommand command) {
        // Validar que el paciente exista
        Patient patient = patientRepositoryPort.findById(command.patientId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Paciente no encontrado con id: " + command.patientId()));

        // RN-03: Validar edad del paciente (no fechas de nacimiento futuras)
        if (patient.getBirthDate() != null && patient.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }

        // Validar que el médico exista
        Doctor doctor = doctorRepositoryPort.findById(command.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + command.doctorId()));

        // Verificar si el paciente está bloqueado por penalizaciones (RN-05)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentPenalties = penaltyRepositoryPort.countByPatientIdAndCreatedAtAfter(command.patientId(), thirtyDaysAgo);
        if (recentPenalties >= 3) {
            throw new IllegalStateException(
                    "El paciente tiene " + recentPenalties + " penalizaciones en los últimos 30 días y no puede agendar nuevas citas");
        }

        // Verificar que la fecha no sea pasada
        if (command.appointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de la cita no puede ser pasada");
        }

        // Validar horarios de la cita
        validateAppointmentHours(command.appointmentDate());

        // Obtener todas las citas no canceladas para verificar conflictos
        LocalDateTime dayStart = command.appointmentDate().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = command.appointmentDate().withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        // Verificar conflicto del médico (intervalos de 30 minutos)
        for (Appointment appt : allAppointments) {
            if (appt.getDoctorId().equals(command.doctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                // Verificar si los horarios se superponen (dentro de 30 minutos)
                long minutesDiff = java.time.Duration.between(appt.getAppointmentDate(), command.appointmentDate()).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException(
                            "El médico ya tiene una cita en el horario seleccionado");
                }
            }
        }

        // RN-04: Verificar conflicto del paciente con el MISMO médico (intervalos de 30 minutos)
        // Un paciente no puede tener dos citas con el mismo médico en el mismo horario
        for (Appointment appt : allAppointments) {
            if (appt.getPatientId().equals(command.patientId()) &&
                    appt.getDoctorId().equals(command.doctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                // Verificar si los horarios se superponen (dentro de 30 minutos)
                long minutesDiff = java.time.Duration.between(appt.getAppointmentDate(), command.appointmentDate()).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException(
                            "El paciente ya tiene una cita con este médico en el horario seleccionado");
                }
            }
        }

        Appointment appointment = Appointment.builder()
                .patientId(command.patientId())
                .doctorId(command.doctorId())
                .appointmentDate(command.appointmentDate())
                .status(AppointmentStatus.PROGRAMADA)
                .notes(command.notes())
                .build();

        Appointment saved = appointmentRepositoryPort.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(CancelAppointmentCommand command) {
        Appointment appointment = getAppointmentById(command.id());

        // Validar que la cita pueda ser cancelada
        if (AppointmentStatus.CANCELADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("La cita ya se encuentra cancelada");
        }
        
        if (AppointmentStatus.FINALIZADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una cita finalizada");
        }

        // Verificar si la cancelación es tardía (menos de 2 horas antes)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate();
        long hoursUntilAppointment = java.time.Duration.between(now, appointmentDateTime).toHours();

        // Establecer fecha/hora de cancelación
        appointment.setCancellationDate(now);

        // Crear penalización solo si la cancelación es con menos de 2 horas de anticipación
        if (hoursUntilAppointment < 2) {
            Penalty penalty = Penalty.builder()
                    .patientId(appointment.getPatientId())
                    .penaltyType("CANCELACION_TARDIA")
                    .description("Cancelación con menos de 2 horas de anticipación. Cita programada para: " + appointmentDateTime)
                    .build();
            penaltyRepositoryPort.save(penalty);
        }

        appointment.setStatus(AppointmentStatus.CANCELADA);
        Appointment updated = appointmentRepositoryPort.save(appointment);

        return mapToResponse(updated);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(RescheduleAppointmentCommand command) {
        // Validar que la cita exista
        Appointment existingAppointment = getAppointmentById(command.id());

        // No se puede reprogramar una cita cancelada
        if (AppointmentStatus.CANCELADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita cancelada");
        }

        // No se puede reprogramar una cita finalizada
        if (AppointmentStatus.FINALIZADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita finalizada");
        }

        // Validar que la nueva fecha no sea pasada
        if (command.newAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de la cita no puede ser pasada");
        }

        // Validar horarios de la cita (horario laboral, intervalos de 30 min, día de la semana)
        validateAppointmentHours(command.newAppointmentDate());

        // Verificar disponibilidad del médico en la nueva fecha
        LocalDateTime dayStart = command.newAppointmentDate().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = command.newAppointmentDate().withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        // Verificar conflicto del médico (intervalos de 30 minutos)
        for (Appointment appt : allAppointments) {
            if (appt.getDoctorId().equals(existingAppointment.getDoctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                long minutesDiff = java.time.Duration.between(appt.getAppointmentDate(), command.newAppointmentDate()).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException(
                            "El médico ya tiene una cita en el horario seleccionado");
                }
            }
        }

        // RN-04: Verificar conflicto del paciente con el MISMO médico (intervalos de 30 minutos)
        // Un paciente no puede tener dos citas con el mismo médico en el mismo horario
        for (Appointment appt : allAppointments) {
            if (appt.getPatientId().equals(existingAppointment.getPatientId()) &&
                    appt.getDoctorId().equals(existingAppointment.getDoctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                long minutesDiff = java.time.Duration.between(appt.getAppointmentDate(), command.newAppointmentDate()).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException(
                            "El paciente ya tiene una cita con este médico en el horario seleccionado");
                }
            }
        }

        // Verificar que la nueva fecha no sea domingo
        java.time.DayOfWeek dayOfWeek = command.newAppointmentDate().getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No es posible agendar citas los domingos");
        }

        // Cancelar la cita anterior con lógica de penalización si aplica
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldAppointmentDate = existingAppointment.getAppointmentDate();
        long hoursUntilOldAppointment = java.time.Duration.between(now, oldAppointmentDate).toHours();

        existingAppointment.setCancellationDate(now);

        // Crear penalización si la cancelación es con menos de 2 horas de anticipación
        if (hoursUntilOldAppointment < 2) {
            Penalty penalty = Penalty.builder()
                    .patientId(existingAppointment.getPatientId())
                    .penaltyType("CANCELACION_TARDIA")
                    .description("Reprogramación con menos de 2 horas de anticipación. Cita original: " + oldAppointmentDate)
                    .build();
            penaltyRepositoryPort.save(penalty);
        }

        existingAppointment.setStatus(AppointmentStatus.CANCELADA);
        appointmentRepositoryPort.save(existingAppointment);

        // Crear nueva cita
        Appointment newAppointment = Appointment.builder()
                .patientId(existingAppointment.getPatientId())
                .doctorId(existingAppointment.getDoctorId())
                .appointmentDate(command.newAppointmentDate())
                .status(AppointmentStatus.PROGRAMADA)
                .notes(existingAppointment.getNotes())
                .build();

        Appointment saved = appointmentRepositoryPort.save(newAppointment);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse updateStatus(UpdateAppointmentStatusCommand command) {
        Appointment appointment = getAppointmentById(command.id());

        // No se puede actualizar una cita cancelada
        if (AppointmentStatus.CANCELADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede actualizar una cita cancelada");
        }

        // No se puede actualizar una cita finalizada
        if (AppointmentStatus.FINALIZADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede actualizar una cita finalizada");
        }

        appointment.setStatus(command.status());
        Appointment updated = appointmentRepositoryPort.save(appointment);

        return mapToResponse(updated);
    }

    private void validateAppointmentHours(LocalDateTime appointmentDate) {
        java.time.DayOfWeek dayOfWeek = appointmentDate.getDayOfWeek();
        int hour = appointmentDate.getHour();
        int minute = appointmentDate.getMinute();

        // Validar intervalos de 30 minutos
        if (minute % 30 != 0) {
            throw new IllegalArgumentException("La hora debe estar en intervalos de 30 minutos (ej: 08:00, 08:30, 09:00)");
        }

        // Domingo no permitido
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No se pueden programar citas los domingos");
        }

        // Lunes a viernes: 08:00 a 18:00
        if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            if (hour < 8 || hour >= 18) {
                throw new IllegalArgumentException("El horario de atención de lunes a viernes es de 08:00 a 18:00");
            }
            return;
        }

        // Sábado: 08:00 a 13:00
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
            if (hour < 8 || hour >= 13) {
                throw new IllegalArgumentException("El horario de atención el sábado es de 08:00 a 13:00");
            }
        }
    }

    public List<AvailableSlot> getAvailableSlots(AvailableSlotsQuery query) {
        // Validar que el médico exista
        Doctor doctor = doctorRepositoryPort.findById(query.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + query.doctorId()));

        List<AvailableSlot> availableSlots = new java.util.ArrayList<>();

        // Iterar por cada día en el rango
        for (LocalDate date = query.fechaInicio(); !date.isAfter(query.fechaFin()); date = date.plusDays(1)) {
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Determinar horas de inicio y fin según el día de la semana
            int startHour;
            int endHour;

            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                startHour = 8;
                endHour = 13;
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                // Domingo: sin citas
                continue;
            } else {
                // Lunes a viernes
                startHour = 8;
                endHour = 18;
            }

            // Generar intervalos de 30 minutos para este día
            for (int hour = startHour; hour < endHour; hour++) {
                for (int minute = 0; minute < 60; minute += 30) {
                    LocalDateTime slotTime = date.atTime(hour, minute);

                    // Verificar si este intervalo ya está reservado
                    LocalDateTime slotStart = slotTime;
                    LocalDateTime slotEnd = slotTime.plusMinutes(30);

                    List<Appointment> existingAppointments =
                            appointmentRepositoryPort.findByAppointmentDateBetween(slotStart, slotEnd);

                    boolean isBooked = false;
                    for (Appointment appt : existingAppointments) {
                        if (appt.getDoctorId().equals(query.doctorId()) &&
                                !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                            isBooked = true;
                            break;
                        }
                    }

                    if (!isBooked) {
                        availableSlots.add(new AvailableSlot(
                                slotTime,
                                date,
                                String.format("%02d:%02d", hour, minute),
                                dayOfWeek.toString()
                        ));
                    }
                }
            }
        }

        return availableSlots;
    }

    @Override
    public List<AppointmentResponse> listAppointments(ListAppointmentsQuery query) {
        // Convertir String status a enum para consulta JPA
        AppointmentStatus statusEnum = null;
        if (query.status() != null && !query.status().isEmpty()) {
            try {
                statusEnum = AppointmentStatus.valueOf(query.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Estado inválido - retornar lista vacía
                return List.of();
            }
        }
        
        List<Appointment> appointments = appointmentRepositoryPort.findByFilters(
                query.doctorId(),
                query.patientId(),
                statusEnum,
                query.startDate(),
                query.endDate());
        
        return appointments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AppointmentResponse mapToResponse(Appointment appointment) {
        Doctor doctor = doctorRepositoryPort.findById(appointment.getDoctorId()).orElse(null);
        Patient patient = patientRepositoryPort.findById(appointment.getPatientId()).orElse(null);

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                patient != null ? patient.getName() : null,
                appointment.getDoctorId(),
                doctor != null ? doctor.getName() : null,
                doctor != null ? doctor.getSpecialty() : null,
                appointment.getAppointmentDate(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCancellationDate(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
