package com.medisalud.appointments.infrastructure.adapter.out.persistence;

import com.medisalud.appointments.domain.model.Patient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPatientRepository implements com.medisalud.appointments.domain.port.out.PatientRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Patient save(Patient patient) {
        if (patient.getId() == null) {
            entityManager.persist(patient);
            return patient;
        }
        return entityManager.merge(patient);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        Patient patient = entityManager.find(Patient.class, id);
        return Optional.ofNullable(patient);
    }

    @Override
    public Optional<Patient> findByIdentityDocument(String identityDocument) {
        List<Patient> result = entityManager.createQuery(
                "SELECT p FROM Patient p WHERE p.identityDocument = :identityDocument", Patient.class)
                .setParameter("identityDocument", identityDocument)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public List<Patient> findAll() {
        return entityManager.createQuery("SELECT p FROM Patient p", Patient.class)
                .getResultList();
    }

    @Override
    public List<Patient> findByActiveTrue() {
        return entityManager.createQuery("SELECT p FROM Patient p WHERE p.active = true", Patient.class)
                .getResultList();
    }

    @Override
    public void deleteById(UUID id) {
        Patient patient = entityManager.find(Patient.class, id);
        if (patient != null) {
            patient.setActive(false);
            entityManager.merge(patient);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Patient patient = entityManager.find(Patient.class, id);
        return patient != null;
    }
}
