package com.medisalud.appointments.domain.port.out;

import com.medisalud.appointments.domain.model.Patient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepositoryPort {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    Optional<Patient> findByIdentityDocument(String identityDocument);
    List<Patient> findAll();
    List<Patient> findByActiveTrue();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
