package com.medisalud.appointments.domain.port.in;

import java.util.UUID;

public record RescheduleAppointmentCommand(
    UUID id,
    java.time.LocalDateTime newAppointmentDate
) {}
