package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record DeletePatientCommand(
    @NotNull(message = "El ID es obligatorio")
    UUID id
) {}
