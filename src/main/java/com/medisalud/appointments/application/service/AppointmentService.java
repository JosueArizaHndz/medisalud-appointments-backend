package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
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
import java.util.List;
import java.util.UUID;

/**
 * Servicio principal orquestador para la gestión de citas médicas.
 * Delega la lógica específica a servicios especializados:
 * - AppointmentConflictValidator: validación de conflictos
 * - AppointmentPenaltyService: lógica de penalizaciones
 * - AppointmentAvailabilityService: validación de horarios y disponibilidad
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService implements AppointmentServiceInterface, AppointmentQueryPort {

    private final AppointmentRepositoryPort appointmentRepositoryPort;
    private final DoctorRepositoryPort doctorRepositoryPort;
    private final PatientRepositoryPort patientRepositoryPort;
    private final PenaltyRepositoryPort penaltyRepositoryPort;

    private final AppointmentConflictValidator conflictValidator;
    private final AppointmentPenaltyService penaltyService;
    private final AppointmentAvailabilityService availabilityService;

    // ==================== Consultas básicas ====================

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

    // ==================== Creación de citas ====================

    @Transactional
    @Override
    public AppointmentResponse createAppointment(CreateAppointmentCommand command) {
        // 1. Validar que el paciente exista
        Patient patient = patientRepositoryPort.findById(command.patientId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Paciente no encontrado con id: " + command.patientId()));

        // RN-03: Validar edad del paciente (no fechas de nacimiento futuras)
        if (patient.getBirthDate() != null && patient.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }

        // 2. Validar que el médico exista
        Doctor doctor = doctorRepositoryPort.findById(command.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + command.doctorId()));

        // 3. Verificar si el paciente está bloqueado por penalizaciones (RN-05)
        if (penaltyService.isPatientBlocked(command.patientId())) {
            long recentPenalties = penaltyService.countRecentPenalties(command.patientId());
            throw new IllegalStateException(
                    "El paciente tiene " + recentPenalties + " penalizaciones en los últimos 30 días y no puede agendar nuevas citas");
        }

        // 4. Verificar que la fecha no sea pasada
        if (command.appointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de la cita no puede ser pasada");
        }

        // 5. Validar horarios (delegate)
        availabilityService.validateAppointmentHours(command.appointmentDate());

        // 6. Validar conflictos (delegate)
        conflictValidator.validateDoctorConflict(command.doctorId(), command.appointmentDate(), null);
        conflictValidator.validatePatientDoctorConflict(command.patientId(), command.doctorId(), command.appointmentDate(), null);

        // 7. Construir y guardar la cita
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

    // ==================== Cancelación de citas ====================

    @Transactional
    @Override
    public AppointmentResponse cancelAppointment(CancelAppointmentCommand command) {
        Appointment appointment = getAppointmentById(command.id());

        // Validar que la cita pueda ser cancelada
        if (AppointmentStatus.CANCELADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("La cita ya se encuentra cancelada");
        }

        if (AppointmentStatus.FINALIZADA.equals(appointment.getStatus())) {
            throw new IllegalStateException("No se puede cancelar una cita finalizada");
        }

        // Establecer fecha/hora de cancelación
        appointment.setCancellationDate(LocalDateTime.now());

        // Delegar creación de penalización si aplica
        String reason = "Cancelación con menos de 2 horas de anticipación. Cita programada para: " + appointment.getAppointmentDate();
        penaltyService.createPenaltyIfLateCancellation(appointment, reason);

        appointment.setStatus(AppointmentStatus.CANCELADA);
        Appointment updated = appointmentRepositoryPort.save(appointment);

        return mapToResponse(updated);
    }

    // ==================== Reprogramación de citas ====================

    @Transactional
    @Override
    public AppointmentResponse rescheduleAppointment(RescheduleAppointmentCommand command) {
        // 1. Validar que la cita exista
        Appointment existingAppointment = getAppointmentById(command.id());

        // No se puede reprogramar una cita cancelada o finalizada
        if (AppointmentStatus.CANCELADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita cancelada");
        }

        if (AppointmentStatus.FINALIZADA.equals(existingAppointment.getStatus())) {
            throw new IllegalStateException("No se puede reprogramar una cita finalizada");
        }

        // 2. Validar que la nueva fecha no sea pasada
        if (command.newAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de la cita no puede ser pasada");
        }

        // 3. Validar horarios (delegate)
        availabilityService.validateAppointmentHours(command.newAppointmentDate());

        // 4. Validar conflictos del médico (delegate)
        // Nota: NO se excluye la cita existente para mantener compatibilidad con el comportamiento original
        conflictValidator.validateDoctorConflict(
                existingAppointment.getDoctorId(),
                command.newAppointmentDate(),
                null);

        // 5. Validar conflictos del paciente+médico (delegate)
        // Nota: NO se excluye la cita existente para mantener compatibilidad con el comportamiento original
        conflictValidator.validatePatientDoctorConflict(
                existingAppointment.getPatientId(),
                existingAppointment.getDoctorId(),
                command.newAppointmentDate(),
                null);

        // 6. Verificar que la nueva fecha no sea domingo (ya validado por validateAppointmentHours, pero se mantiene para claridad)
        java.time.DayOfWeek dayOfWeek = command.newAppointmentDate().getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No es posible agendar citas los domingos");
        }

        // 7. Cancelar la cita anterior con lógica de penalización si aplica
        existingAppointment.setCancellationDate(LocalDateTime.now());
        String reason = "Reprogramación con menos de 2 horas de anticipación. Cita original: " + existingAppointment.getAppointmentDate();
        penaltyService.createPenaltyIfLateCancellation(existingAppointment, reason);

        existingAppointment.setStatus(AppointmentStatus.CANCELADA);
        appointmentRepositoryPort.save(existingAppointment);

        // 8. Crear nueva cita
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

    // ==================== Actualización de estado ====================

    @Transactional
    @Override
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

    // ==================== Disponibilidad ====================

    @Override
    public List<AvailableSlot> getAvailableSlots(AvailableSlotsQuery query) {
        return availabilityService.getAvailableSlots(query);
    }

    // ==================== Listado con filtros ====================

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

    // ==================== Mapeo ====================

    @Override
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
