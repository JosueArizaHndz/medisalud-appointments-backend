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

    // Use a fixed Monday in the future to avoid day-of-week issues
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

    @Test
    void mondayMorningValidAppointment_shouldSucceed() {
        // Monday at 08:00 is valid
        LocalDateTime mondayMorning = getMondayFuture(8, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayMorning, "Morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status());
    }

    @Test
    void mondayAfternoonValidAppointment_shouldSucceed() {
        // Monday at 17:30 is valid (before 18:00)
        LocalDateTime mondayAfternoon = getMondayFuture(17, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, mondayAfternoon, "Afternoon appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status());
    }

    @Test
    void mondayEveningInvalidAppointment_shouldFail() {
        // Monday at 18:00 is NOT valid (must be before 18:00)
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

    @Test
    void mondayEarlyMorningInvalidAppointment_shouldFail() {
        // Monday at 07:30 is NOT valid (must be after 08:00)
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

    @Test
    void saturdayMorningValidAppointment_shouldSucceed() {
        // Saturday at 08:00 is valid
        LocalDateTime saturdayMorning = getSaturdayFuture(8, 0);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, saturdayMorning, "Saturday morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status());
    }

    @Test
    void saturdayLateMorningValidAppointment_shouldSucceed() {
        // Saturday at 12:30 is valid (before 13:00)
        LocalDateTime saturdayLate = getSaturdayFuture(12, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, saturdayLate, "Saturday late morning appointment"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status());
    }

    @Test
    void saturdayAfternoonInvalidAppointment_shouldFail() {
        // Saturday at 13:00 is NOT valid (must be before 13:00)
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

    @Test
    void sundayAnyTimeInvalidAppointment_shouldFail() {
        // Sunday at any time is NOT valid
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

    @Test
    void validThirtyMinuteIntervals_shouldSucceed() {
        // Test various valid 30-minute intervals
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
            assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status(),
                "Time " + time + " should be valid");
        }
    }

    @Test
    void fridayLastValidSlot_shouldSucceed() {
        // Friday at 17:30 is the last valid slot
        LocalDateTime fridayLast = getFridayFuture(17, 30);
        CreateAppointmentCommand cmd = new CreateAppointmentCommand(
            patientId, doctorId, fridayLast, "Friday last slot"
        );
        AppointmentResponse response = appointmentService.createAppointment(cmd);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response.status());
    }
}
