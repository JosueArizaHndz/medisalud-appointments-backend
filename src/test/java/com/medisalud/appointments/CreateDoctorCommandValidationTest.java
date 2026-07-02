package com.medisalud.appointments;

import com.medisalud.appointments.domain.port.in.CreateDoctorCommand;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateDoctorCommandValidationTest {

    private final Validator validator;

    CreateDoctorCommandValidationTest() {
        ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void shouldPassWhenAllFieldsAreValid() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "Dr. Juan Pérez",
            "juan@email.com",
            "CARDIOLOGIA",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "No debería haber violaciones de validación");
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "",
            "juan@email.com",
            "CARDIOLOGIA",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.size() >= 1, "Debe haber al menos una violación");
        boolean hasNameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        assertTrue(hasNameViolation, "Debe haber violación en el campo name");
    }

    @Test
    void shouldFailWhenNameTooShort() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "Dr",
            "juan@email.com",
            "CARDIOLOGIA",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.size() >= 1, "Debe haber al menos una violación");
        boolean hasSizeViolation = violations.stream()
            .anyMatch(v -> v.getMessage().contains("3"));
        assertTrue(hasSizeViolation, "Debe haber violación de longitud mínima");
    }

    @Test
    void shouldFailWhenEmailIsInvalid() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "Dr. Juan Pérez",
            "invalid-email",
            "CARDIOLOGIA",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.size() >= 1, "Debe haber al menos una violación");
        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailViolation, "Debe haber violación en el campo email");
    }

    @Test
    void shouldFailWhenEmailIsBlank() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "Dr. Juan Pérez",
            "",
            "CARDIOLOGIA",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.size() >= 1, "Debe haber al menos una violación");
        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailViolation, "Debe haber violación en el campo email");
    }

    @Test
    void shouldFailWhenSpecialtyIsBlank() {
        CreateDoctorCommand command = new CreateDoctorCommand(
            "Dr. Juan Pérez",
            "juan@email.com",
            "",
            "MED-001",
            30
        );

        Set<ConstraintViolation<CreateDoctorCommand>> violations = validator.validate(command);
        assertTrue(violations.size() >= 1, "Debe haber al menos una violación");
        boolean hasSpecialtyViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("specialty"));
        assertTrue(hasSpecialtyViolation, "Debe haber violación en el campo specialty");
    }
}
