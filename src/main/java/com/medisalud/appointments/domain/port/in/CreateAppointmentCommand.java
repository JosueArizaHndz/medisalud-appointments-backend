package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentCommand(
    @NotNull(message = "Debe seleccionar un paciente")
    UUID patientId,

    @NotNull(message = "Debe seleccionar un doctor")
    UUID doctorId,

    @NotNull(message = "La fecha es obligatoria")
    LocalDateTime appointmentDate,

    String notes
) {}
