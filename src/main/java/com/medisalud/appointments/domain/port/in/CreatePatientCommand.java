package com.medisalud.appointments.domain.port.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Schema(description = "Command para crear un nuevo paciente")
public record CreatePatientCommand(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre completo del paciente", example = "Juan Pérez", minLength = 3, maxLength = 100)
    String name,

    @NotBlank(message = "El documento es obligatorio")
    @Size(min = 7, max = 20, message = "El documento debe tener entre 7 y 20 caracteres")
    @Schema(description = "Número de documento de identidad", example = "1234567890", minLength = 7, maxLength = 20)
    String identityDocument,

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{7,}$", message = "El teléfono debe tener mínimo 7 dígitos")
    @Schema(description = "Número de teléfono", example = "3001234567")
    String phone,

    @Schema(description = "Fecha de nacimiento (opcional)", example = "1990-01-15")
    java.time.LocalDate birthDate,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Email de contacto", example = "juan.perez@email.com")
    String email,

    @PositiveOrZero(message = "El número de historias médicas debe ser positivo o cero")
    @Schema(description = "Número de historia médica", example = "12345")
    Integer medicalRecordNumber
) {}
