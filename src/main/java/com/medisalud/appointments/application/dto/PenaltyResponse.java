package com.medisalud.appointments.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PenaltyResponse(
    UUID id,
    UUID patientId,
    String penaltyType,
    String description,
    LocalDateTime createdAt
) {}
