package com.medisalud.appointments.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Franja horaria disponible")
public record AvailableSlot(
    @Schema(description = "Fecha y hora de la franja disponible", example = "2026-07-06T09:00:00")
    LocalDateTime slot,
    
    @Schema(description = "Fecha de la franja", example = "2026-07-06")
    LocalDate date,
    
    @Schema(description = "Hora de la franja", example = "09:00")
    String time,
    
    @Schema(description = "Día de la semana", example = "MONDAY")
    String dayOfWeek
) {}
