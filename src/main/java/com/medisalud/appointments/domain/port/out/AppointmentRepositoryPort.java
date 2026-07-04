package com.medisalud.appointments.domain.port.out;

import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findAll();
    List<Appointment> findByDoctorId(UUID doctorId);
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByAppointmentDateBetween(LocalDateTime start, LocalDateTime end);
    void deleteById(UUID id);
    boolean existsById(UUID id);
    List<Appointment> findByFilters(UUID doctorId, UUID patientId, AppointmentStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
