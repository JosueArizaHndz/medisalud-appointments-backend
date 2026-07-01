package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record UpdatePatientCommand(
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    String phone,

    java.time.LocalDate birthDate,

    @Email(message = "El email debe ser válido")
    String email,

    Integer medicalRecordNumber
) {}
