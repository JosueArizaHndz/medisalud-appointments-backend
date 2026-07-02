package com.medisalud.appointments.domain.port.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Schema(description = "Command para actualizar un paciente existente")
public record UpdatePatientCommand(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre completo del paciente", example = "Juan Pérez", minLength = 3, maxLength = 100)
    String name,

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(description = "Número de teléfono", example = "3001234567")
    String phone,

    @Schema(description = "Fecha de nacimiento (opcional)", example = "1990-01-15")
    java.time.LocalDate birthDate,

    @Email(message = "El email debe ser válido")
    @Schema(description = "Email de contacto", example = "juan.perez@email.com")
    String email,

    @PositiveOrZero(message = "El número de historias médicas debe ser positivo o cero")
    @Schema(description = "Número de historia médica", example = "12345")
    Integer medicalRecordNumber
) {}
