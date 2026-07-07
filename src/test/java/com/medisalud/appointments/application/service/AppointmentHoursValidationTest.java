package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.CreateAppointmentCommand;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import com.medisalud.appointments.domain.port.out.PatientRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentHoursValidationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepositoryPort doctorRepositoryPort;

    @Autowired
    private PatientRepositoryPort patientRepositoryPort;

    @Autowired
    private AppointmentRepositoryPort appointmentRepositoryPort;

    private UUID doctorId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        Doctor doctor = Doctor.builder()
                .name("Dr. Test Doctor")
                .email("test.doctor@medisalud.com")
                .specialty("GENERAL")
                .licenseNumber("TEST001")
                .maxPatients(50)
                .active(true)
                .build();
        doctor = doctorRepositoryPort.save(doctor);
        doctorId = doctor.getId();

        Patient patient = Patient.builder()
                .name("Patient Test")
                .identityDocument("1111111111")
                .phone("3001111111")
                .email("patient@test.com")
                .active(true)
                .build();
        patient = patientRepositoryPort.save(patient);
        patientId = patient.getId();
    }

    /**
     * Test base para validar la configuración inicial de horarios de citas.
     * Limpia las citas existentes para asegurar un estado limpio de prueba.
     */
    @Test
    void testAppointmentHoursValidation() {
        // Limpiar citas existentes para asegurar un estado limpio de prueba
        appointmentRepositoryPort.findAll().forEach(a -> appointmentRepositoryPort.deleteById(a.getId()));
    }

    // Usar un lunes fijo en el futuro para evitar problemas de validación de día de la semana
    private LocalDateTime getMondayFuture(int hour, int minute) {
        return LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(hour, minute);
    }

    private LocalDateTime getSaturdayFuture(int hour, int minute) {
        return LocalDate.now().plusDays(9).with(java.time.DayOfWeek.SATURDAY).atTime(hour, minute);
    }

    private LocalDateTime getSundayFuture(int hour, int minute) {
        return LocalDate.now().plusDays(8).with(java.time.DayOfWeek.SUNDAY).atTime(hour, minute);
    }

    private LocalDateTime getFridayFuture(int hour, int minute) {
        return LocalDate.now().plusDays(6).with(java.time.DayOfWeek.FRIDAY).atTime(hour, minute);
    }

    /**
     * Verifica que se pueda crear una cita el lunes a las 08:00 (primer horario válido).
     */
    @Test
    void mondayMorningValidAppointment_shouldSucceed() {
        // Lunes a las 08:00 es válido
        LocalDateTime mondayMorning = getMondayFuture(8, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayMorning, "Morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA, response.status());
    }

    /**
     * Verifica que se pueda crear una cita el lunes a las 17:30 (último horario válido antes de las 18:00).
     */
    @Test
    void mondayAfternoonValidAppointment_shouldSucceed() {
        // Lunes a las 17:30 es válido (antes de 18:00)
        LocalDateTime mondayAfternoon = getMondayFuture(17, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayAfternoon, "Afternoon appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA, response.status());
    }

    /**
     * Verifica que NO se pueda crear una cita el lunes a las 18:00 o después
     * (horario inválido para días de semana).
     */
    @Test
    void mondayEveningInvalidAppointment_shouldFail() {
        // Lunes a las 18:00 NO es válido (debe ser antes de 18:00)
        LocalDateTime mondayEvening = getMondayFuture(18, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayEvening, "Evening appointment"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("08:00 a 18:00"), "Should mention weekday hours");
    }

    /**
     * Verifica que NO se pueda crear una cita el lunes antes de las 08:00
     * (horario inválido para días de semana).
     */
    @Test
    void mondayEarlyMorningInvalidAppointment_shouldFail() {
        // Lunes a las 07:30 NO es válido (debe ser después de 08:00)
        LocalDateTime mondayEarly = getMondayFuture(7, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayEarly, "Early morning appointment"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("08:00 a 18:00"), "Should mention weekday hours");
    }

    /**
     * Verifica que se pueda crear una cita el sábado a las 08:00 (primer horario válido para sábados).
     */
    @Test
    void saturdayMorningValidAppointment_shouldSucceed() {
        // Sábado a las 08:00 es válido
        LocalDateTime saturdayMorning = getSaturdayFuture(8, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, saturdayMorning, "Saturday morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA, response.status());
    }

    /**
     * Verifica que se pueda crear una cita el sábado a las 12:30 (último horario válido antes de las 13:00).
     */
    @Test
    void saturdayLateMorningValidAppointment_shouldSucceed() {
        // Sábado a las 12:30 es válido (antes de 13:00)
        LocalDateTime saturdayLate = getSaturdayFuture(12, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, saturdayLate, "Saturday late morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA, response.status());
    }

    /**
     * Verifica que NO se pueda crear una cita el sábado a las 13:00 o después
     * (horario inválido para sábados).
     */
    @Test
    void saturdayAfternoonInvalidAppointment_shouldFail() {
        // Sábado a las 13:00 NO es válido (debe ser antes de 13:00)
        LocalDateTime saturdayAfternoon = getSaturdayFuture(13, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, saturdayAfternoon, "Saturday afternoon appointment"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("08:00 a 13:00"), "Should mention Saturday hours");
    }

    /**
     * Verifica que NO se pueda crear una cita ningún día domingo (no hay atención).
     */
    @Test
    void sundayAnyTimeInvalidAppointment_shouldFail() {
        // Domingo a cualquier hora NO es válido
        LocalDateTime sundayMorning = getSundayFuture(10, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, sundayMorning, "Sunday appointment"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("domingos"), "Should mention Sundays are not allowed");
    }

    /**
     * Verifica que NO se pueda crear una cita en un horario que no sea
     * múltiplo de 30 minutos (ej. 10:15 no es válido).
     */
    @Test
    void nonThirtyMinuteIntervalInvalidAppointment_shouldFail() {
        // 10:15 is NOT valid (must be 30-minute intervals)
        LocalDateTime invalidTime = getMondayFuture(10, 15);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, invalidTime, "Invalid interval appointment"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("30 minutos"), "Should mention 30-minute intervals");
    }

    /**
     * Verifica que NO se pueda crear una cita en un horario que no sea
     * múltiplo de 30 minutos (ej. 11:47 no es válido).
     */
    @Test
    void nonThirtyMinuteIntervalInvalidAppointment2_shouldFail() {
        // 11:47 is NOT valid (must be 30-minute intervals)
        LocalDateTime invalidTime = getMondayFuture(11, 47);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, invalidTime, "Invalid interval appointment 2"
        );
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> appointmentService.createAppointment(cmd)
        );
        assertTrue(exception.getMessage().contains("30 minutos"), "Should mention 30-minute intervals");
    }

    /**
     * Verifica que varios horarios válidos con intervalos de 30 minutos
     * sean aceptados correctamente (08:00, 08:30, 09:00, 09:30, 10:00, 17:30).
     */
    @Test
    void validThirtyMinuteIntervals_shouldSucceed() {
        // Probar varios intervalos válidos de 30 minutos
        LocalDateTime[] validTimes = {
            getMondayFuture(8, 0),
            getMondayFuture(8, 30),
            getMondayFuture(9, 0),
            getMondayFuture(9, 30),
            getMondayFuture(10, 0),
            getMondayFuture(17, 30)
        };

        for (LocalDateTime time : validTimes) {
            CreateAppointmentCommand cmd = new CreateAppointmentCommand(
                patientId, doctorId, time, "Valid interval appointment"
            );
            AppointmentResponse response = appointmentService.createAppointment(cmd);
            assertEquals(AppointmentStatus.PROGRAMADA, response.status(),
                "Time " + time + " should be valid");
        }
    }

    /**
     * Verifica que el último slot válido del viernes (17:30) sea aceptado correctamente.
     */
    @Test
    void fridayLastValidSlot_shouldSucceed() {
        // Viernes a las 17:30 es el último slot válido
        LocalDateTime fridayLast = getFridayFuture(17, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, fridayLast, "Friday last slot"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA, response.status());
    }
}
