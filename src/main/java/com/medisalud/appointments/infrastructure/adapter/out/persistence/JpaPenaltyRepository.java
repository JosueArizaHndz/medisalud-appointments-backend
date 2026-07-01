package com.medisalud.appointments.infrastructure.adapter.out.persistence;

import com.medisalud.appointments.domain.model.Penalty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaPenaltyRepository implements com.medisalud.appointments.domain.port.out.PenaltyRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Penalty save(Penalty penalty) {
        entityManager.persist(penalty);
        return penalty;
    }

    @Override
    public List<Penalty> findByPatientId(UUID patientId) {
        return entityManager.createQuery(
                "SELECT p FROM Penalty p WHERE p.patientId = :patientId", Penalty.class)
                .setParameter("patientId", patientId)
                .getResultList();
    }

    @Override
    public void deleteById(UUID id) {
        Penalty penalty = entityManager.find(Penalty.class, id);
        if (penalty != null) {
            entityManager.remove(penalty);
        }
    }
}
