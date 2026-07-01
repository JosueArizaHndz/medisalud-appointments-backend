package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;

public record CreateDoctorCommand(
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,

    @NotBlank(message = "La especialidad es obligatoria")
    String specialty,

    String licenseNumber,

    Integer maxPatients
) {}
