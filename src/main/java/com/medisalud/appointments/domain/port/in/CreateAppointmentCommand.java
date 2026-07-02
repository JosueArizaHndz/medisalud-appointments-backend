package com.medisalud.appointments.domain.port.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Command para reservar una nueva cita médica")
public record CreateAppointmentCommand(
    @NotNull(message = "Debe seleccionar un paciente")
    @Schema(description = "ID del paciente", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID patientId,

    @NotNull(message = "Debe seleccionar un doctor")
    @Schema(description = "ID del médico", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    UUID doctorId,

    @NotNull(message = "La fecha es obligatoria")
    @Schema(description = "Fecha y hora de la cita", example = "2026-07-15T10:30:00")
    LocalDateTime appointmentDate,

    @Schema(description = "Notas adicionales de la cita", example = "Consulta por dolor de cabeza")
    String notes
) {}
