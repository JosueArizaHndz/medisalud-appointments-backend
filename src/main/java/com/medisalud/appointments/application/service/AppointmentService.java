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
        // Validate patient exists
        Patient patient = patientRepositoryPort.findById(command.patientId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Paciente no encontrado con id: " + command.patientId()));

        // RN-03: Validate patient age (no future birth dates)
        if (patient.getBirthDate() != null && patient.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }

        // Validate doctor exists
        Doctor doctor = doctorRepositoryPort.findById(command.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + command.doctorId()));

        // Check if patient is blocked due to penalties (RN-05)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentPenalties = penaltyRepositoryPort.countByPatientIdAndCreatedAtAfter(command.patientId(), thirtyDaysAgo);
        if (recentPenalties >= 3) {
            throw new IllegalStateException(
                    "El paciente tiene " + recentPenalties + " penalizaciones en los últimos 30 días y no puede agendar nuevas citas");
        }

        // Check date is not in the past
        if (command.appointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de la cita no puede ser pasada");
        }

        // Validate appointment hours
        validateAppointmentHours(command.appointmentDate());

        // Get all non-cancelled appointments for conflict checking
        LocalDateTime dayStart = command.appointmentDate().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = command.appointmentDate().withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        // Check doctor conflict (30-minute intervals)
        for (Appointment appt : allAppointments) {
            if (appt.getDoctorId().equals(command.doctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                // Check if time slots overlap (within 30 minutes of each other)
                long minutesDiff = java.time.Duration.between(appt.getAppointmentDate(), command.appointmentDate()).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException(
                            "El médico ya tiene una cita en el horario seleccionado");
                }
            }
        }

        // RN-04: Check patient conflict with the SAME doctor (30-minute intervals)
        // A patient cannot have two appointments with the SAME doctor in the same time slot
        for (Appointment appt : allAppointments) {
            if (appt.getPatientId().equals(command.patientId()) &&
                    appt.getDoctorId().equals(command.doctorId()) &&
                    !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                // Check if time slots overlap (within 30 minutes of each other)
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

        // Validate appointment can be cancelled
        if (AppointmentStatus.CANCELADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("La cita ya se encuentra cancelada");
        }
        
        if (AppointmentStatus.FINALIZADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una cita finalizada");
        }

        // Check if cancellation is late (less than 2 hours before appointment)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate();
        long hoursUntilAppointment = java.time.Duration.between(now, appointmentDateTime).toHours();

        // Set cancellation date/time
        appointment.setCancellationDate(now);

        // Create penalty only if cancellation is less than 2 hours before appointment
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
        // Validate appointment exists
        Appointment existingAppointment = getAppointmentById(command.id());

        // Cannot reschedule cancelled appointments
        if (AppointmentStatus.CANCELADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita cancelada");
        }

        // Cannot reschedule finalized appointments
        if (AppointmentStatus.FINALIZADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita finalizada");
        }

        // Validate new date is not in the past
        if (command.newAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de la cita no puede ser pasada");
        }

        // Validate appointment hours (business hours, 30-min intervals, day of week)
        validateAppointmentHours(command.newAppointmentDate());

        // Check doctor availability at new date
        LocalDateTime dayStart = command.newAppointmentDate().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = command.newAppointmentDate().withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        // Check doctor conflict (30-minute intervals)
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

        // RN-04: Check patient conflict with the SAME doctor (30-minute intervals)
        // A patient cannot have two appointments with the SAME doctor in the same time slot
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

        // Check if new date is on a Sunday
        java.time.DayOfWeek dayOfWeek = command.newAppointmentDate().getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No es posible agendar citas los domingos");
        }

        // Cancel the old appointment with penalty logic if applicable
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldAppointmentDate = existingAppointment.getAppointmentDate();
        long hoursUntilOldAppointment = java.time.Duration.between(now, oldAppointmentDate).toHours();

        existingAppointment.setCancellationDate(now);

        // Create penalty if cancellation is less than 2 hours before appointment
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

        // Create new appointment
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

        // Cannot update cancelled appointments
        if (AppointmentStatus.CANCELADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede actualizar una cita cancelada");
        }

        // Cannot update finalized appointments
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

        // Validate 30-minute intervals
        if (minute % 30 != 0) {
            throw new IllegalArgumentException("La hora debe estar en intervalos de 30 minutos (ej: 08:00, 08:30, 09:00)");
        }

        // Sunday is not allowed
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No se pueden programar citas los domingos");
        }

        // Monday to Friday: 08:00 to 18:00
        if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            if (hour < 8 || hour >= 18) {
                throw new IllegalArgumentException("El horario de atención de lunes a viernes es de 08:00 a 18:00");
            }
            return;
        }

        // Saturday: 08:00 to 13:00
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
            if (hour < 8 || hour >= 13) {
                throw new IllegalArgumentException("El horario de atención el sábado es de 08:00 a 13:00");
            }
        }
    }

    public List<AvailableSlot> getAvailableSlots(AvailableSlotsQuery query) {
        // Validate doctor exists
        Doctor doctor = doctorRepositoryPort.findById(query.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + query.doctorId()));

        List<AvailableSlot> availableSlots = new java.util.ArrayList<>();

        // Iterate through each day in the range
        for (LocalDate date = query.fechaInicio(); !date.isAfter(query.fechaFin()); date = date.plusDays(1)) {
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Determine start and end hours based on day of week
            int startHour;
            int endHour;

            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                startHour = 8;
                endHour = 13;
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                // Sunday: no appointments
                continue;
            } else {
                // Monday to Friday
                startHour = 8;
                endHour = 18;
            }

            // Generate 30-minute slots for this day
            for (int hour = startHour; hour < endHour; hour++) {
                for (int minute = 0; minute < 60; minute += 30) {
                    LocalDateTime slotTime = date.atTime(hour, minute);

                    // Check if this slot is already booked
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
        // Convert String status to enum for JPA query
        AppointmentStatus statusEnum = null;
        if (query.status() != null && !query.status().isEmpty()) {
            try {
                statusEnum = AppointmentStatus.valueOf(query.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status - return empty list
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
