package com.medisalud.appointments.domain.port.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Command para consultar disponibilidad de un médico")
public record AvailableSlotsQuery(
    @NotNull(message = "El ID del médico es obligatorio")
    @Schema(description = "ID del médico", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    UUID doctorId,

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(description = "Fecha de inicio del rango", example = "2026-07-06")
    LocalDate fechaInicio,

    @NotNull(message = "La fecha de fin es obligatoria")
    @Schema(description = "Fecha de fin del rango", example = "2026-07-10")
    LocalDate fechaFin
) {}
