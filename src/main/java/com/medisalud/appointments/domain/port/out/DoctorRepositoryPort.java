package com.medisalud.appointments.domain.port.out;

import com.medisalud.appointments.domain.model.Doctor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepositoryPort {
    Doctor save(Doctor doctor);
    Optional<Doctor> findById(UUID id);
    List<Doctor> findAll();
    List<Doctor> findByActiveTrue();
    Optional<Doctor> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
