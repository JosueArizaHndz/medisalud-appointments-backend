package com.medisalud.appointments.infrastructure.adapter.out.persistence;

import com.medisalud.appointments.domain.model.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDoctorRepository implements com.medisalud.appointments.domain.port.out.DoctorRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Doctor save(Doctor doctor) {
        if (doctor.getId() == null) {
            entityManager.persist(doctor);
            return doctor;
        }
        return entityManager.merge(doctor);
    }

    @Override
    public Optional<Doctor> findById(UUID id) {
        Doctor doctor = entityManager.find(Doctor.class, id);
        return Optional.ofNullable(doctor);
    }

    @Override
    public List<Doctor> findAll() {
        return entityManager.createQuery("SELECT d FROM Doctor d", Doctor.class)
                .getResultList();
    }

    @Override
    public List<Doctor> findByActiveTrue() {
        return entityManager.createQuery("SELECT d FROM Doctor d WHERE d.active = true", Doctor.class)
                .getResultList();
    }

    @Override
    public Optional<Doctor> findByEmail(String email) {
        List<Doctor> result = entityManager.createQuery(
                "SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                .setParameter("email", email)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(d) FROM Doctor d WHERE d.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public void deleteById(UUID id) {
        Doctor doctor = entityManager.find(Doctor.class, id);
        if (doctor != null) {
            doctor.setActive(false);
            entityManager.merge(doctor);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Doctor doctor = entityManager.find(Doctor.class, id);
        return doctor != null;
    }
}
