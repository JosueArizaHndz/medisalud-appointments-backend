package com.medisalud.appointments.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    String name,
    String identityDocument,
    String phone,
    LocalDate birthDate,
    String email,
    Integer medicalRecordNumber,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
