package com.medisalud.appointments.infrastructure.adapter.out.persistence;

import com.medisalud.appointments.domain.model.Appointment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAppointmentRepository implements com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Appointment save(Appointment appointment) {
        if (appointment.getId() == null) {
            entityManager.persist(appointment);
            return appointment;
        }
        return entityManager.merge(appointment);
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        Appointment appointment = entityManager.find(Appointment.class, id);
        return Optional.ofNullable(appointment);
    }

    @Override
    public List<Appointment> findAll() {
        return entityManager.createQuery("SELECT a FROM Appointment a", Appointment.class)
                .getResultList();
    }

    @Override
    public List<Appointment> findByDoctorId(UUID doctorId) {
        return entityManager.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctorId = :doctorId", Appointment.class)
                .setParameter("doctorId", doctorId)
                .getResultList();
    }

    @Override
    public List<Appointment> findByPatientId(UUID patientId) {
        return entityManager.createQuery(
                "SELECT a FROM Appointment a WHERE a.patientId = :patientId", Appointment.class)
                .setParameter("patientId", patientId)
                .getResultList();
    }

    @Override
    public List<Appointment> findByAppointmentDateBetween(LocalDateTime start, LocalDateTime end) {
        return entityManager.createQuery(
                "SELECT a FROM Appointment a WHERE a.appointmentDate >= :start AND a.appointmentDate < :end", Appointment.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @Override
    public void deleteById(UUID id) {
        Appointment appointment = entityManager.find(Appointment.class, id);
        if (appointment != null) {
            appointment.setStatus("CANCELADA");
            entityManager.merge(appointment);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Appointment appointment = entityManager.find(Appointment.class, id);
        return appointment != null;
    }
}
