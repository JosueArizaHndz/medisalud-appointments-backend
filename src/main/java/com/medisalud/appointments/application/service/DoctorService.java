package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.DoctorResponse;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.port.in.*;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService implements DoctorServiceInterface, DoctorQueryPort {

    private final DoctorRepositoryPort doctorRepositoryPort;

    @Override
    public List<Doctor> getAllDoctors() {
        return doctorRepositoryPort.findAll();
    }

    @Override
    public Doctor getDoctorById(UUID id) {
        return doctorRepositoryPort.findById(id)
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + id));
    }

    @Override
    public List<Doctor> getActiveDoctors() {
        return doctorRepositoryPort.findByActiveTrue();
    }

    @Override
    public Doctor getDoctorByEmail(String email) {
        return doctorRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con email: " + email));
    }

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorCommand command) {
        if (doctorRepositoryPort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Ya existe un doctor con ese email");
        }

        Doctor doctor = Doctor.builder()
                .name(command.name())
                .email(command.email())
                .specialty(command.specialty())
                .phone(command.phone())
                .licenseNumber(command.licenseNumber())
                .maxPatients(command.maxPatients() != null ? command.maxPatients() : 20)
                .active(true)
                .build();

        Doctor saved = doctorRepositoryPort.save(doctor);
        return mapToResponse(saved);
    }

    @Transactional
    public DoctorResponse updateDoctor(UUID id, UpdateDoctorCommand command) {
        Doctor doctor = getDoctorById(id);

        doctor.setName(command.name());
        doctor.setEmail(command.email());
        doctor.setSpecialty(command.specialty());
        doctor.setPhone(command.phone());
        doctor.setLicenseNumber(command.licenseNumber());
        doctor.setMaxPatients(command.maxPatients());

        Doctor updated = doctorRepositoryPort.save(doctor);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteDoctor(DeleteDoctorCommand command) {
        if (!doctorRepositoryPort.existsById(command.id())) {
            throw new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                    "Médico no encontrado con id: " + command.id());
        }
        doctorRepositoryPort.deleteById(command.id());
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getEmail(),
                doctor.getSpecialty(),
                doctor.getPhone(),
                doctor.getLicenseNumber(),
                doctor.getMaxPatients(),
                doctor.getActive(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt()
        );
    }
}
