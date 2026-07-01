package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record DeleteDoctorCommand(
    @NotNull(message = "El ID es obligatorio")
    UUID id
) {}
