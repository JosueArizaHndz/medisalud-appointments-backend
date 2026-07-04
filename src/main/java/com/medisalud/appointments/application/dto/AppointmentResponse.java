package com.medisalud.appointments.application.dto;

import com.medisalud.appointments.domain.enums.AppointmentStatus;
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
    AppointmentStatus status,
    String notes,
    LocalDateTime cancellationDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
