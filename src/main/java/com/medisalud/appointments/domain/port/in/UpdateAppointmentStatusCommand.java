package com.medisalud.appointments.domain.port.in;

import com.medisalud.appointments.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateAppointmentStatusCommand(
    @NotNull(message = "El ID es obligatorio")
    UUID id,
    
    @NotNull(message = "El estado es obligatorio")
    AppointmentStatus status
) {}
