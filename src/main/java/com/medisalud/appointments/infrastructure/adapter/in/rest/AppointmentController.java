package com.medisalud.appointments.infrastructure.adapter.in.rest;

import com.medisalud.appointments.application.dto.ApiResponse;
import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.application.service.AppointmentService;
import com.medisalud.appointments.domain.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments() {
        List<com.medisalud.appointments.domain.model.Appointment> appointments = appointmentService.getAllAppointments();
        List<AppointmentResponse> responses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatientId(), null, a.getDoctorId(), null, null,
                        a.getAppointmentDate(), a.getStatus(), a.getNotes(),
                        a.getCreatedAt(), a.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Citas obtenidas exitosamente", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable String id) {
        var appointment = appointmentService.getAppointmentById(java.util.UUID.fromString(id));
        var response = appointmentService.createAppointment(
                new CreateAppointmentCommand(
                        appointment.getPatientId(),
                        appointment.getDoctorId(),
                        appointment.getAppointmentDate(),
                        appointment.getNotes()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByDoctor(@PathVariable String doctorId) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByDoctorId(java.util.UUID.fromString(doctorId));
        List<AppointmentResponse> responses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatientId(), null, a.getDoctorId(), null, null,
                        a.getAppointmentDate(), a.getStatus(), a.getNotes(),
                        a.getCreatedAt(), a.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByPatient(@PathVariable String patientId) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByPatientId(java.util.UUID.fromString(patientId));
        List<AppointmentResponse> responses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatientId(), null, a.getDoctorId(), null, null,
                        a.getAppointmentDate(), a.getStatus(), a.getNotes(),
                        a.getCreatedAt(), a.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByDate(date);
        List<AppointmentResponse> responses = appointments.stream()
                .map(a -> new AppointmentResponse(
                        a.getId(), a.getPatientId(), null, a.getDoctorId(), null, null,
                        a.getAppointmentDate(), a.getStatus(), a.getNotes(),
                        a.getCreatedAt(), a.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Citas para la fecha " + date, responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentCommand command) {
        AppointmentResponse response = appointmentService.createAppointment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(java.util.UUID.fromString(id)));
        return ResponseEntity.ok(ApiResponse.ok("Cita cancelada exitosamente", response));
    }
}
