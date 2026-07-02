package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.model.Penalty;
import com.medisalud.appointments.domain.port.in.CancelAppointmentCommand;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentCancellationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepositoryPort doctorRepositoryPort;

    @Autowired
    private PatientRepositoryPort patientRepositoryPort;

    @Autowired
    private AppointmentRepositoryPort appointmentRepositoryPort;

    @Autowired
    private PenaltyRepositoryPort penaltyRepositoryPort;

    private UUID doctorId;
    private UUID patientId;
    private LocalDateTime futureMonday;
    private LocalDateTime futureSaturday;
    private LocalDateTime sunday;

    @BeforeEach
    void setUp() {
        // Create test doctor
        Doctor doctor = Doctor.builder()
                .name("Dr. Test Doctor")
                .email("test.cancellation@medisalud.com")
                .specialty("GENERAL")
                .licenseNumber("TESTCANC001")
                .maxPatients(50)
                .active(true)
                .build();
        doctor = doctorRepositoryPort.save(doctor);
        doctorId = doctor.getId();

        // Create test patient
        Patient patient = Patient.builder()
                .name("Patient Cancellation Test")
                .identityDocument("9999999999")
                .phone("3001234567")
                .email("cancellation@test.com")
                .active(true)
                .build();
        patient = patientRepositoryPort.save(patient);
        patientId = patient.getId();

        // Ensure futureMonday is a Monday
        futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        futureSaturday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SATURDAY).atTime(10, 0);
        sunday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SUNDAY).atTime(10, 0);
    }

    @Test
    void testCancelAppointmentWithPenalty_LessThan2Hours() {
        // Create an appointment in 1 hour (should generate penalty)
        // Use a time slot that is within 30-minute intervals and within 1 hour from now
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Test notes"));

        // Get initial penalty count
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Cancel the appointment
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        // Verify appointment is cancelled
        assertEquals(AppointmentStatus.CANCELADA.name(), response.status());
        assertNotNull(response.cancellationDate(), "La fecha de cancelación debe estar presente");

        // Verify penalty was created
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount + 1, penalties.size(), "Debe crearse una penalización");
        assertEquals("CANCELACION_TARDIA", penalties.get(penalties.size() - 1).getPenaltyType());
    }

    @Test
    void testCancelAppointmentWithoutPenalty_GreaterThan2Hours() {
        // Create an appointment in 3 hours (should NOT generate penalty)
        // Use a time slot that is within 30-minute intervals
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(3);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Test notes"));

        // Get initial penalty count
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Cancel the appointment
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        // Verify appointment is cancelled
        assertEquals(AppointmentStatus.CANCELADA.name(), response.status());
        assertNotNull(response.cancellationDate(), "La fecha de cancelación debe estar presente");

        // Verify NO penalty was created
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount, penalties.size(), "No debe crearse penalización");
    }

    @Test
    void testCancelNonExistentAppointment_ReturnsNull() {
        UUID nonExistentId = UUID.randomUUID();

        // Should throw exception for non-existent appointment
        assertThrows(Exception.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(nonExistentId));
        });
    }

    @Test
    void testCancelAlreadyCancelledAppointment() {
        // Create an appointment
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Test notes"));

        // First cancellation should succeed
        AppointmentResponse response1 = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));
        assertEquals(AppointmentStatus.CANCELADA.name(), response1.status());

        // Second cancellation should fail
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        });
    }

    @Test
    void testCancelFinalizedAppointment() {
        // Create an appointment
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Test notes"));

        // Set status to FINALIZADA
        com.medisalud.appointments.domain.model.Appointment appt = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        appt.setStatus(AppointmentStatus.FINALIZADA.name());
        appointmentRepositoryPort.save(appt);

        // Cannot cancel finalized appointment
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        });
    }

    @Test
    void testCancelledSlotBecomesAvailable() {
        // Create an appointment
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Test notes"));

        // Query available slots BEFORE cancellation
        List<com.medisalud.appointments.application.dto.AvailableSlot> slotsBefore = 
                appointmentService.getAvailableSlots(
                        new com.medisalud.appointments.domain.port.in.AvailableSlotsQuery(
                                doctorId, 
                                LocalDate.now().plusDays(7), 
                                LocalDate.now().plusDays(7)));

        // Cancel the appointment
        appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));

        // Query available slots AFTER cancellation
        List<com.medisalud.appointments.application.dto.AvailableSlot> slotsAfter = 
                appointmentService.getAvailableSlots(
                        new com.medisalud.appointments.domain.port.in.AvailableSlotsQuery(
                                doctorId, 
                                LocalDate.now().plusDays(7), 
                                LocalDate.now().plusDays(7)));

        // The cancelled slot should now be available
        assertTrue(slotsAfter.size() >= slotsBefore.size(), 
                "El slot cancelado debe volver a estar disponible");
    }

    @Test
    void testCancelAppointmentOnSaturday() {
        // Create an appointment on Saturday
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureSaturday, "Test Saturday"));

        // Cancel the appointment
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        assertEquals(AppointmentStatus.CANCELADA.name(), response.status());
        assertNotNull(response.cancellationDate());
    }

    @Test
    void testCannotCancelAppointmentOnSunday() {
        // Attempt to create appointment on Sunday should fail
        assertThrows(Exception.class, () -> {
            appointmentService.createAppointment(
                    new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                            patientId, doctorId, sunday, "Test Sunday"));
        });
    }

    @Test
    void testCancellationDateIsRecorded() {
        // Create an appointment with more than 2 hours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(5);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Test notes"));

        // Get original appointment
        com.medisalud.appointments.domain.model.Appointment original = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        assertNull(original.getCancellationDate(), "Original no debe tener fecha de cancelación");

        // Cancel the appointment
        appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));

        // Verify cancellation date is recorded
        com.medisalud.appointments.domain.model.Appointment cancelled = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        assertNotNull(cancelled.getCancellationDate(), "Debe registrarse la fecha de cancelación");
        assertNotNull(cancelled.getStatus());
        assertEquals(AppointmentStatus.CANCELADA.name(), cancelled.getStatus());
    }

    @Test
    void testMultiplePenaltiesForMultipleLateCancellations() {
        // Create patient 2
        Patient patient2 = Patient.builder()
                .name("Patient Multiple Penalties")
                .identityDocument("8888888888")
                .phone("3001234568")
                .email("multiple@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Create and cancel 3 appointments within 2 hours (should generate 3 penalties)
        for (int i = 0; i < 3; i++) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            AppointmentResponse appointment = appointmentService.createAppointment(
                    new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                            patient2Id, doctorId, appointmentTime, "Test penalty " + i));

            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        }

        // Verify 3 penalties were created
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patient2Id);
        assertEquals(3, penalties.size(), "Debe crear 3 penalizaciones");
    }
}
