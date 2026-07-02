package com.medisalud.appointments.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientId,
    String patientName,
    UUID doctorId,
    String doctorName,
    String doctorSpecialty,
    LocalDateTime appointmentDate,
    String status,
    String notes,
    LocalDateTime cancellationDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
