package com.medisalud.appointments.domain.port.in;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreatePatientCommandValidationTest {

    private final Validator validator;

    {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void validCreatePatientCommand_shouldPassValidation() {
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "No debería haber violaciones de validación");
    }

    @Test
    void blankName_shouldFailValidation() {
        CreatePatientCommand command = new CreatePatientCommand(
            "",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long nameViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("name"))
            .count();
        assertTrue(nameViolations > 0, "Nombre en blanco debería fallar");
    }

    @Test
    void shortName_shouldFailValidation() {
        CreatePatientCommand command = new CreatePatientCommand(
            "Ju",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long nameViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("name"))
            .count();
        assertTrue(nameViolations > 0, "Nombre menor a 3 caracteres debería fallar");
    }

    @Test
    void blankIdentityDocument_shouldFailValidation() {
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long docViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("identityDocument"))
            .count();
        assertTrue(docViolations > 0, "Documento en blanco debería fallar");
    }

    @Test
    void invalidEmail_shouldFailValidation() {
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "correo-invalido",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long emailViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("email"))
            .count();
        assertTrue(emailViolations > 0, "Email inválido debería fallar");
    }

    @Test
    void blankEmail_shouldFailValidation() {
        // Email es obligatorio en pacientes
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            "",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long emailViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("email"))
            .count();
        assertTrue(emailViolations > 0, "Email en blanco debería fallar (es obligatorio)");
    }

    @Test
    void nullEmail_shouldFailValidation() {
        // Email es obligatorio en pacientes
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "3001234567",
            LocalDate.of(1990, 1, 15),
            null,
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long emailViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("email"))
            .count();
        assertTrue(emailViolations > 0, "Email nulo debería fallar (es obligatorio)");
    }

    @Test
    void blankPhone_shouldFailValidation() {
        // Teléfono es obligatorio en pacientes
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long phoneViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("phone"))
            .count();
        assertTrue(phoneViolations > 0, "Teléfono en blanco debería fallar (es obligatorio)");
    }

    @Test
    void nullPhone_shouldFailValidation() {
        // Teléfono es obligatorio en pacientes
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            null,
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long phoneViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("phone"))
            .count();
        assertTrue(phoneViolations > 0, "Teléfono nulo debería fallar (es obligatorio)");
    }

    @Test
    void shortPhone_shouldFailValidation() {
        // Teléfono con menos de 7 dígitos debe fallar
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "123",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long phoneViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("phone"))
            .count();
        assertTrue(phoneViolations > 0, "Teléfono con menos de 7 dígitos debería fallar");
    }

    @Test
    void valid7DigitPhone_shouldPassValidation() {
        // Teléfono con exactamente 7 dígitos debe pasar
        CreatePatientCommand command = new CreatePatientCommand(
            "Juan Pérez",
            "1234567890",
            "1234567",
            LocalDate.of(1990, 1, 15),
            "juan.perez@email.com",
            12345
        );

        Set<ConstraintViolation<CreatePatientCommand>> violations = validator.validate(command);
        long phoneViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("phone"))
            .count();
        assertTrue(phoneViolations == 0, "Teléfono con 7 dígitos debería pasar");
    }
}
