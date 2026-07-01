package com.medisalud.appointments.infrastructure.adapter.in.rest;

import com.medisalud.appointments.application.dto.ApiResponse;
import com.medisalud.appointments.application.dto.DoctorResponse;
import com.medisalud.appointments.application.service.DoctorServiceInterface;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorServiceInterface doctorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        List<DoctorResponse> responses = doctors.stream()
                .map(d -> new DoctorResponse(
                        d.getId(), d.getName(), d.getEmail(), d.getSpecialty(),
                        d.getLicenseNumber(), d.getMaxPatients(), d.getActive(),
                        d.getCreatedAt(), d.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Doctores obtenidos exitosamente", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable String id) {
        Doctor doctor = doctorService.getDoctorById(java.util.UUID.fromString(id));
        DoctorResponse response = new DoctorResponse(
                doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getSpecialty(),
                doctor.getLicenseNumber(), doctor.getMaxPatients(), doctor.getActive(),
                doctor.getCreatedAt(), doctor.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getActiveDoctors() {
        List<Doctor> doctors = doctorService.getActiveDoctors();
        List<DoctorResponse> responses = doctors.stream()
                .map(d -> new DoctorResponse(
                        d.getId(), d.getName(), d.getEmail(), d.getSpecialty(),
                        d.getLicenseNumber(), d.getMaxPatients(), d.getActive(),
                        d.getCreatedAt(), d.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Doctores activos obtenidos exitosamente", responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(@Valid @RequestBody CreateDoctorCommand command) {
        DoctorResponse response = doctorService.createDoctor(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable String id,
            @Valid @RequestBody UpdateDoctorCommand command) {
        DoctorResponse response = doctorService.updateDoctor(java.util.UUID.fromString(id), command);
        return ResponseEntity.ok(ApiResponse.ok("Doctor actualizado exitosamente", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable String id) {
        doctorService.deleteDoctor(new DeleteDoctorCommand(java.util.UUID.fromString(id)));
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
