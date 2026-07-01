package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.PatientResponse;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.*;
import com.medisalud.appointments.domain.port.out.PatientRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService implements PatientQueryPort {

    private final PatientRepositoryPort patientRepositoryPort;

    @Override
    public List<Patient> getAllPatients() {
        return patientRepositoryPort.findAll();
    }

    @Override
    public Patient getPatientById(UUID id) {
        return patientRepositoryPort.findById(id)
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Patient not found with id: " + id));
    }

    @Override
    public Patient getPatientByIdentityDocument(String identityDocument) {
        return patientRepositoryPort.findByIdentityDocument(identityDocument)
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Patient not found with identity document: " + identityDocument));
    }

    @Override
    public List<Patient> getActivePatients() {
        return patientRepositoryPort.findByActiveTrue();
    }

    @Transactional
    public PatientResponse createPatient(CreatePatientCommand command) {
        Patient patient = Patient.builder()
                .name(command.name())
                .identityDocument(command.identityDocument())
                .phone(command.phone())
                .birthDate(command.birthDate())
                .email(command.email())
                .medicalRecordNumber(command.medicalRecordNumber() != null ? command.medicalRecordNumber() : 0)
                .active(true)
                .build();

        Patient saved = patientRepositoryPort.save(patient);
        return mapToResponse(saved);
    }

    @Transactional
    public PatientResponse updatePatient(UUID id, UpdatePatientCommand command) {
        Patient patient = getPatientById(id);

        patient.setName(command.name());
        patient.setPhone(command.phone());
        patient.setBirthDate(command.birthDate());
        patient.setEmail(command.email());
        patient.setMedicalRecordNumber(command.medicalRecordNumber());

        Patient updated = patientRepositoryPort.save(patient);
        return mapToResponse(updated);
    }

    @Transactional
    public void deletePatient(DeletePatientCommand command) {
        if (!patientRepositoryPort.existsById(command.id())) {
            throw new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                    "Patient not found with id: " + command.id());
        }
        patientRepositoryPort.deleteById(command.id());
    }

    private PatientResponse mapToResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getIdentityDocument(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getEmail(),
                patient.getMedicalRecordNumber(),
                patient.getActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
