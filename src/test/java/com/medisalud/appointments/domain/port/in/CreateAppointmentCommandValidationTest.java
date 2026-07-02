package com.medisalud.appointments.domain.port.in;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateAppointmentCommandValidationTest {

    private final Validator validator;

    {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void validCreateAppointmentCommand_shouldPassValidation() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDateTime futureDate = LocalDateTime.now().plusDays(7);

        CreateAppointmentCommand command = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate,
            "Consulta de control"
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "No debería haber violaciones de validación");
    }

    @Test
    void nullPatientId_shouldFailValidation() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
            null,
            UUID.randomUUID(),
            LocalDateTime.now().plusDays(7),
            "Consulta de control"
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        long patientViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("patientId"))
            .count();
        assertTrue(patientViolations > 0, "patientId nulo debería fallar");
    }

    @Test
    void nullDoctorId_shouldFailValidation() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
            UUID.randomUUID(),
            null,
            LocalDateTime.now().plusDays(7),
            "Consulta de control"
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        long doctorViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("doctorId"))
            .count();
        assertTrue(doctorViolations > 0, "doctorId nulo debería fallar");
    }

    @Test
    void nullAppointmentDate_shouldFailValidation() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "Consulta de control"
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        long dateViolations = violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("appointmentDate"))
            .count();
        assertTrue(dateViolations > 0, "appointmentDate nulo debería fallar");
    }

    @Test
    void nullNotes_shouldPassValidation() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now().plusDays(7),
            null
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "notes nulo debería pasar (es opcional)");
    }

    @Test
    void validCommandWithoutNotes_shouldPassValidation() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now().plusDays(7),
            null
        );

        Set<ConstraintViolation<CreateAppointmentCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "Comando válido sin notas debería pasar");
    }
}
