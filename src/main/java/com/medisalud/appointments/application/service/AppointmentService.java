package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService implements AppointmentQueryPort {

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
                        "Appointment not found with id: " + id));
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
                        "Patient not found with id: " + command.patientId()));

        // Validate doctor exists
        Doctor doctor = doctorRepositoryPort.findById(command.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Doctor not found with id: " + command.doctorId()));

        // Check date conflict
        LocalDateTime start = command.appointmentDate().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = command.appointmentDate().withHour(23).withMinute(59).withSecond(59);
        List<Appointment> existing = appointmentRepositoryPort.findByAppointmentDateBetween(start, end);

        for (Appointment appt : existing) {
            if (appt.getDoctorId().equals(command.doctorId()) &&
                    !AppointmentStatus.CANCELADA.name().equals(appt.getStatus())) {
                throw new IllegalArgumentException(
                        "El doctor ya tiene una cita programada en ese horario");
            }
        }

        Appointment appointment = Appointment.builder()
                .patientId(command.patientId())
                .doctorId(command.doctorId())
                .appointmentDate(command.appointmentDate())
                .status(AppointmentStatus.PROGRAMADA.name())
                .notes(command.notes())
                .build();

        Appointment saved = appointmentRepositoryPort.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(CancelAppointmentCommand command) {
        Appointment appointment = getAppointmentById(command.id());

        if (AppointmentStatus.FINALIZADA.name().equals(appointment.getStatus()) ||
                AppointmentStatus.CANCELADA.name().equals(appointment.getStatus())) {
            throw new IllegalStateException("La cita no puede ser cancelada en este estado");
        }

        appointment.setStatus(AppointmentStatus.CANCELADA.name());
        Appointment updated = appointmentRepositoryPort.save(appointment);

        // Create penalty for patient (no-show or late cancellation)
        Penalty penalty = Penalty.builder()
                .patientId(appointment.getPatientId())
                .penaltyType("CANCELACION_TARDIA")
                .description("Cita cancelada: " + command.id())
                .build();
        penaltyRepositoryPort.save(penalty);

        return mapToResponse(updated);
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
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
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
