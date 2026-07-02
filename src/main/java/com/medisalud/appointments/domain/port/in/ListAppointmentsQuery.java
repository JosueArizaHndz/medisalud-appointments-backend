package com.medisalud.appointments.domain.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListAppointmentsQuery(
    UUID doctorId,
    UUID patientId,
    String status,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
