package com.medisalud.appointments.infrastructure.adapter.in.rest;

import com.medisalud.appointments.application.dto.ApiResponse;
import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.application.service.AppointmentServiceInterface;
import com.medisalud.appointments.domain.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "API para gestión de citas médicas")
public class AppointmentController {

    private final AppointmentServiceInterface appointmentService;

    @GetMapping
    @Operation(
        summary = "Consultar citas médicas con filtros",
        description = "Obtiene lista de citas médicas. Todos los filtros son opcionales.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Citas obtenidas exitosamente")
        }
    )
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments(
            @Parameter(description = "ID del médico")
            @RequestParam(required = false) UUID doctorId,
            
            @Parameter(description = "ID del paciente")
            @RequestParam(required = false) UUID patientId,
            
            @Parameter(description = "Estado de la cita (PROGRAMADA, CONFIRMADA, EN_PROCESO, FINALIZADA, CANCELADA, NO_ASISTIO)")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Fecha/hora inicio del rango")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "Fecha/hora fin del rango")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        // Use JPQL filtering instead of in-memory streaming for better performance
        List<AppointmentResponse> responses = appointmentService.listAppointments(
                new com.medisalud.appointments.domain.port.in.ListAppointmentsQuery(
                        doctorId, patientId, status, startDate, endDate));
        
        return ResponseEntity.ok(ApiResponse.ok("Citas obtenidas exitosamente", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable String id) {
        var appointment = appointmentService.getAppointmentById(java.util.UUID.fromString(id));
        var response = appointmentService.mapToResponse(appointment);
        return ResponseEntity.ok(ApiResponse.ok("Cita obtenida exitosamente", response));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByDoctor(@PathVariable String doctorId) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByDoctorId(java.util.UUID.fromString(doctorId));
        List<AppointmentResponse> responses = appointments.stream()
                .map(appointmentService::mapToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByPatient(@PathVariable String patientId) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByPatientId(java.util.UUID.fromString(patientId));
        List<AppointmentResponse> responses = appointments.stream()
                .map(appointmentService::mapToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<com.medisalud.appointments.domain.model.Appointment> appointments =
                appointmentService.getAppointmentsByDate(date);
        List<AppointmentResponse> responses = appointments.stream()
                .map(appointmentService::mapToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Citas para la fecha " + date, responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentCommand command) {
        AppointmentResponse response = appointmentService.createAppointment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
        summary = "Cancelar una cita médica",
        description = "Cancela una cita existente. Si la cancelación es con menos de 2 horas de anticipación, se registra una penalización.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cita cancelada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cita no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cita ya cancelada o finalizada")
        }
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(java.util.UUID.fromString(id)));
        return ResponseEntity.ok(ApiResponse.ok("Cita cancelada exitosamente", response));
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(
        summary = "Reprogramar una cita médica",
        description = "Cambia la fecha/hora de una cita. Cancela la cita anterior y crea una nueva. Si la reprogramación es con menos de 2 horas de anticipación, se registra penalización.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cita reprogramada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cita no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cita ya cancelada o finalizada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Fecha inválida o conflicto de horario")
        }
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            @PathVariable String id,
            @Parameter(description = "Nueva fecha/hora de la cita", required = true, example = "2026-07-15T10:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime newDate) {
        
        AppointmentResponse response = appointmentService.rescheduleAppointment(
                new com.medisalud.appointments.domain.port.in.RescheduleAppointmentCommand(
                        java.util.UUID.fromString(id), newDate));
        return ResponseEntity.ok(ApiResponse.ok("Cita reprogramada exitosamente", response));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(
        summary = "Confirmar una cita médica",
        description = "Cambia el estado de la cita a CONFIRMADA.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cita confirmada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cita no encontrada")
        }
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirmAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.updateStatus(
                new com.medisalud.appointments.domain.port.in.UpdateAppointmentStatusCommand(
                        java.util.UUID.fromString(id),
                        com.medisalud.appointments.domain.enums.AppointmentStatus.CONFIRMADA));
        return ResponseEntity.ok(ApiResponse.ok("Cita confirmada exitosamente", response));
    }

    @PatchMapping("/{id}/attend")
    @Operation(
        summary = "Marcar una cita como atendida",
        description = "Cambia el estado de la cita a ATENDIDA.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cita marcada como atendida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cita no encontrada")
        }
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> attendAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.updateStatus(
                new com.medisalud.appointments.domain.port.in.UpdateAppointmentStatusCommand(
                        java.util.UUID.fromString(id),
                        com.medisalud.appointments.domain.enums.AppointmentStatus.ATENDIDA));
        return ResponseEntity.ok(ApiResponse.ok("Cita marcada como atendida exitosamente", response));
    }

    @PatchMapping("/{id}/finalize")
    @Operation(
        summary = "Finalizar una cita médica",
        description = "Cambia el estado de la cita a FINALIZADA.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cita finalizada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cita no encontrada")
        }
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> finalizeAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.updateStatus(
                new com.medisalud.appointments.domain.port.in.UpdateAppointmentStatusCommand(
                        java.util.UUID.fromString(id),
                        com.medisalud.appointments.domain.enums.AppointmentStatus.FINALIZADA));
        return ResponseEntity.ok(ApiResponse.ok("Cita finalizada exitosamente", response));
    }

    @GetMapping("/availability")
    @Operation(
        summary = "Consultar disponibilidad de un médico",
        description = "Obtiene las franjas horarias disponibles para un médico en un rango de fechas",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Disponibilidad obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Médico no encontrado")
        }
    )
    public ResponseEntity<ApiResponse<List<AvailableSlot>>> getAvailableSlots(
            @Parameter(description = "ID del médico", required = true)
            @RequestParam UUID doctorId,
            
            @Parameter(description = "Fecha de inicio del rango", required = true, example = "2026-07-06")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            
            @Parameter(description = "Fecha de fin del rango", required = true, example = "2026-07-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(doctorId, fechaInicio, fechaFin);
        List<AvailableSlot> availableSlots = appointmentService.getAvailableSlots(query);
        return ResponseEntity.ok(ApiResponse.ok("Disponibilidad obtenida exitosamente", availableSlots));
    }
}
