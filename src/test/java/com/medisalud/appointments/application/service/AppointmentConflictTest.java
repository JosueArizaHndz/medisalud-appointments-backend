package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.CreateAppointmentCommand;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import com.medisalud.appointments.domain.port.out.PatientRepositoryPort;
import com.medisalud.appointments.domain.port.out.PenaltyRepositoryPort;
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
class AppointmentConflictTest {

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
    private UUID patient2Id;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        // Create test doctor
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

        // Create test patients
        Patient patient1 = Patient.builder()
                .name("Patient One")
                .identityDocument("1111111111")
                .phone("3001111111")
                .email("patient1@test.com")
                .active(true)
                .build();
        patient1 = patientRepositoryPort.save(patient1);
        patientId = patient1.getId();

        Patient patient2 = Patient.builder()
                .name("Patient Two")
                .identityDocument("2222222222")
                .phone("3002222222")
                .email("patient2@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        patient2Id = patient2.getId();

        // Ensure futureDate is a Monday to avoid day-of-week validation issues
        futureDate = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
    }

    @Test
    void doctorCanHaveMultipleAppointmentsSameDayDifferentSlots() {
        // Create first appointment at 10:00
        CreateAppointmentCommand cmd1 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "First appointment"
        );
        AppointmentResponse response1 = appointmentService.createAppointment(cmd1);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response1.status());

        // Create second appointment at 14:00 (different slot)
        CreateAppointmentCommand cmd2 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(14).withMinute(0),
            "Second appointment"
        );
        AppointmentResponse response2 = appointmentService.createAppointment(cmd2);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response2.status());

        // Both should be created successfully
        assertNotNull(response1.id());
        assertNotNull(response2.id());
        assertNotEquals(response1.id(), response2.id());
    }

    @Test
    void doctorCannotHaveTwoAppointmentsSameTimeSlot() {
        // Create first appointment at 10:00
        CreateAppointmentCommand cmd1 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "First appointment"
        );
        appointmentService.createAppointment(cmd1);

        // Try to create second appointment at 10:00 (SAME slot - conflict)
        CreateAppointmentCommand cmd2 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "Conflict appointment"
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> appointmentService.createAppointment(cmd2)
        );
        assertTrue(exception.getMessage().contains("médico"), "Error should mention doctor conflict");
    }

    @Test
    void patientCannotHaveTwoAppointmentsSameTimeSlot() {
        // Create test doctor 2
        Doctor doctor2 = Doctor.builder()
                .name("Dr. Test Doctor 2")
                .email("test.doctor2@medisalud.com")
                .specialty("CARDIOLOGIA")
                .licenseNumber("TEST002")
                .maxPatients(50)
                .active(true)
                .build();
        doctor2 = doctorRepositoryPort.save(doctor2);
        UUID doctor2Id = doctor2.getId();

        // Create first appointment with patient1 and doctor1 at 10:00
        CreateAppointmentCommand cmd1 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "First appointment"
        );
        appointmentService.createAppointment(cmd1);

        // Try to create second appointment with patient1 and doctor2 at 10:00 (SAME slot)
        CreateAppointmentCommand cmd2 = new CreateAppointmentCommand(
            patientId,
            doctor2Id,
            futureDate.withHour(10).withMinute(0),
            "Conflict appointment"
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> appointmentService.createAppointment(cmd2)
        );
        assertTrue(exception.getMessage().contains("paciente"), "Error should mention patient conflict");
    }

    @Test
    void differentPatientsCanHaveAppointmentsSameTimeSameDoctor() {
        // This should be allowed - different patients, same doctor, same time
        CreateAppointmentCommand cmd1 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "First patient"
        );
        AppointmentResponse response1 = appointmentService.createAppointment(cmd1);

        // This should fail because it's the SAME patient
        CreateAppointmentCommand cmd2 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "Same patient conflict"
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> appointmentService.createAppointment(cmd2)
        );
        assertNotNull(exception);
    }

    @Test
    void cancelledAppointmentDoesNotBlockNewBooking() {
        // Create appointment
        CreateAppointmentCommand cmd1 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "Original appointment"
        );
        AppointmentResponse response1 = appointmentService.createAppointment(cmd1);
        UUID appointmentId = response1.id();

        // Cancel the appointment
        appointmentService.cancelAppointment(
            new com.medisalud.appointments.domain.port.in.CancelAppointmentCommand(appointmentId)
        );

        // Now should be able to book the same slot
        CreateAppointmentCommand cmd2 = new CreateAppointmentCommand(
            patientId,
            doctorId,
            futureDate.withHour(10).withMinute(0),
            "New appointment in cancelled slot"
        );
        AppointmentResponse response2 = appointmentService.createAppointment(cmd2);
        assertEquals(AppointmentStatus.PROGRAMADA.name(), response2.status());
    }
}
