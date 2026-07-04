package com.medisalud.appointments.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorResponse(
    UUID id,
    String name,
    String email,
    String specialty,
    String phone,
    String licenseNumber,
    Integer maxPatients,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
