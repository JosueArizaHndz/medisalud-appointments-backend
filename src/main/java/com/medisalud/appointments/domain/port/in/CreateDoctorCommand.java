package com.medisalud.appointments.domain.port.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Command para crear un nuevo médico")
public record CreateDoctorCommand(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre completo del médico", example = "Dra. María González", minLength = 3, maxLength = 100)
    String name,

    @Schema(description = "Email de contacto", example = "maria.gonzalez@medisalud.com")
    @Email(message = "El email debe tener un formato válido")
    String email,

    @NotBlank(message = "La especialidad es obligatoria")
    @Schema(description = "Especialidad médica", example = "Cardiología")
    String specialty,

    @Schema(description = "Teléfono de contacto", example = "555-1001")
    @Pattern(regexp = "^\\d{7,}$", message = "El teléfono debe tener mínimo 7 dígitos")
    String phone,

    @Schema(description = "Número de licencia médica", example = "MED-001")
    String licenseNumber,

    @Schema(description = "Número máximo de pacientes", example = "30")
    Integer maxPatients
) {}
