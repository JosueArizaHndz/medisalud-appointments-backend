package com.medisalud.appointments.infrastructure.adapter.in.rest;

import com.medisalud.appointments.application.dto.ApiResponse;
import com.medisalud.appointments.application.dto.PatientResponse;
import com.medisalud.appointments.application.service.PatientServiceInterface;
import com.medisalud.appointments.domain.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientServiceInterface patientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<com.medisalud.appointments.domain.model.Patient> patients = patientService.getAllPatients();
        List<PatientResponse> responses = patients.stream()
                .map(p -> new PatientResponse(
                        p.getId(), p.getName(), p.getIdentityDocument(), p.getPhone(),
                        p.getBirthDate(), p.getEmail(), p.getMedicalRecordNumber(),
                        p.getActive(), p.getCreatedAt(), p.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Pacientes obtenidos exitosamente", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable String id) {
        var patient = patientService.getPatientById(java.util.UUID.fromString(id));
        PatientResponse response = new PatientResponse(
                patient.getId(), patient.getName(), patient.getIdentityDocument(), patient.getPhone(),
                patient.getBirthDate(), patient.getEmail(), patient.getMedicalRecordNumber(),
                patient.getActive(), patient.getCreatedAt(), patient.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/search/identity/{identityDocument}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientByIdentityDocument(@PathVariable String identityDocument) {
        var patient = patientService.getPatientByIdentityDocument(identityDocument);
        PatientResponse response = new PatientResponse(
                patient.getId(), patient.getName(), patient.getIdentityDocument(), patient.getPhone(),
                patient.getBirthDate(), patient.getEmail(), patient.getMedicalRecordNumber(),
                patient.getActive(), patient.getCreatedAt(), patient.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(@Valid @RequestBody CreatePatientCommand command) {
        PatientResponse response = patientService.createPatient(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientCommand command) {
        PatientResponse response = patientService.updatePatient(java.util.UUID.fromString(id), command);
        return ResponseEntity.ok(ApiResponse.ok("Paciente actualizado exitosamente", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable String id) {
        patientService.deletePatient(new DeletePatientCommand(java.util.UUID.fromString(id)));
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
