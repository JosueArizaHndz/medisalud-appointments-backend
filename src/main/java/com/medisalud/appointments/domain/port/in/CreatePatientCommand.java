package com.medisalud.appointments.domain.port.in;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreatePatientCommand(
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @NotBlank(message = "La cédula es obligatoria")
    String identityDocument,

    String phone,

    java.time.LocalDate birthDate,

    @Email(message = "El email debe ser válido")
    String email,

    Integer medicalRecordNumber
) {}
