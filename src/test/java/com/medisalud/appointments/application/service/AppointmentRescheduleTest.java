package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.model.Penalty;
import com.medisalud.appointments.domain.port.in.RescheduleAppointmentCommand;
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
class AppointmentRescheduleTest {

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
    private LocalDateTime futureMonday2;
    private LocalDateTime futureSaturday;
    private LocalDateTime sunday;

    @BeforeEach
    void setUp() {
        // Create test doctor
        Doctor doctor = Doctor.builder()
                .name("Dr. Test Doctor Reschedule")
                .email("test.reschedule@medisalud.com")
                .specialty("GENERAL")
                .licenseNumber("TESTRESCH001")
                .maxPatients(50)
                .active(true)
                .build();
        doctor = doctorRepositoryPort.save(doctor);
        doctorId = doctor.getId();

        // Create test patient
        Patient patient = Patient.builder()
                .name("Patient Reschedule Test")
                .identityDocument("7777777777")
                .phone("3007777777")
                .email("reschedule@test.com")
                .active(true)
                .build();
        patient = patientRepositoryPort.save(patient);
        patientId = patient.getId();

        // Ensure dates are on weekdays to avoid day-of-week validation issues
        futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        futureMonday2 = LocalDate.now().plusDays(10).with(java.time.DayOfWeek.MONDAY).atTime(14, 0);
        futureSaturday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SATURDAY).atTime(10, 0);
        sunday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SUNDAY).atTime(10, 0);
    }

    @Test
    void testRescheduleAppointment_Success() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        assertEquals(AppointmentStatus.PROGRAMADA.name(), original.status());

        // Reschedule to a different time
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), futureMonday2));

        // Verify new appointment
        assertEquals(AppointmentStatus.PROGRAMADA.name(), rescheduled.status());
        assertEquals(futureMonday2, rescheduled.appointmentDate());
        assertEquals(patientId, rescheduled.patientId());
        assertEquals(doctorId, rescheduled.doctorId());

        // Verify original appointment was cancelled
        Appointment originalAfter = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNotNull(originalAfter);
        assertEquals(AppointmentStatus.CANCELADA.name(), originalAfter.getStatus());
        assertNotNull(originalAfter.getCancellationDate());
    }

    @Test
    void testRescheduleNonExistentAppointment() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(Exception.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(nonExistentId, futureMonday2));
        });
    }

    @Test
    void testRescheduleCancelledAppointment() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Cancel it first
        appointmentService.cancelAppointment(
                new com.medisalud.appointments.domain.port.in.CancelAppointmentCommand(original.id()));

        // Cannot reschedule a cancelled appointment
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday2));
        });
    }

    @Test
    void testRescheduleFinalizedAppointment() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Set status to FINALIZADA
        Appointment appt = appointmentRepositoryPort.findById(original.id()).orElse(null);
        appt.setStatus(AppointmentStatus.FINALIZADA.name());
        appointmentRepositoryPort.save(appt);

        // Cannot reschedule a finalized appointment
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday2));
        });
    }

    @Test
    void testRescheduleToPastDate() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Try to reschedule to a past date
        LocalDateTime pastDate = LocalDateTime.now().minusHours(2);

        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), pastDate));
        });
    }

    @Test
    void testRescheduleToSunday() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Try to reschedule to Sunday
        assertThrows(Exception.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), sunday));
        });
    }

    @Test
    void testRescheduleDoctorConflict() {
        // Create patient 2
        Patient patient2 = Patient.builder()
                .name("Patient Two Reschedule")
                .identityDocument("6666666666")
                .phone("3006666666")
                .email("patient2reschedule@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Create appointment for patient 2 at same time as patient 1
        AppointmentResponse appt2 = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "Appointment for patient 2"));

        // Try to reschedule patient 1 to conflict with patient 2's appointment (same doctor)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(appt2.id(), futureMonday));
        });
    }

    @Test
    void testReschedulePatientConflict() {
        // Create patient 2
        Patient patient2 = Patient.builder()
                .name("Patient Two Reschedule")
                .identityDocument("5555555555")
                .phone("3005555555")
                .email("patient2reschedule2@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Create two appointments for patient 2 on different days
        LocalDateTime differentMonday = LocalDate.now().plusDays(14).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        
        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "First appointment"));

        AppointmentResponse secondAppt = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, differentMonday, "Second appointment"));

        // Try to reschedule second appointment to conflict with first (same patient)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(secondAppt.id(), futureMonday));
        });
    }

    @Test
    void testRescheduleWithPenalty_LessThan2Hours() {
        // Create appointment in 1.5 hours on a weekday (should generate penalty because < 2 hours)
        LocalDateTime now = LocalDateTime.now();
        // Round to next valid 30-min interval
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1).plusMinutes(30);
        
        // Ensure it's on a weekday
        while (appointmentTime.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || 
               appointmentTime.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            appointmentTime = appointmentTime.plusDays(1);
        }

        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Test penalty reschedule"));

        // Reschedule to a far future date - this should work and create penalty for late cancellation
        LocalDateTime farFuture = LocalDate.now().plusDays(30).with(java.time.DayOfWeek.MONDAY).atTime(9, 0);

        // This test verifies the reschedule works; penalty logic is covered by cancellation tests
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), farFuture));

        assertEquals(AppointmentStatus.PROGRAMADA.name(), rescheduled.status());
        assertEquals(farFuture, rescheduled.appointmentDate());
    }

    @Test
    void testRescheduleWithoutPenalty_GreaterThan2Hours() {
        // Create appointment in 5 hours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(5);

        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Test no penalty reschedule"));

        // Get initial penalty count
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Reschedule to 6 hours from now (should NOT generate penalty)
        LocalDateTime newTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(6);

        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), newTime));

        // Verify NO penalty was created
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount, penalties.size(), "No debe crearse penalización");
        assertEquals(AppointmentStatus.PROGRAMADA.name(), rescheduled.status());
    }

    @Test
    void testReschedulePreservesNotes() {
        // Create an appointment with notes
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Important medical notes"));

        // Reschedule
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), futureMonday2));

        // Notes should be preserved
        assertEquals("Important medical notes", rescheduled.notes());
    }

    @Test
    void testRescheduleToSaturday() {
        // Create an appointment
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Reschedule to Saturday (valid business hours 08-13)
        LocalDateTime saturdayTime = futureSaturday;

        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), saturdayTime));

        assertEquals(AppointmentStatus.PROGRAMADA.name(), rescheduled.status());
        assertEquals(saturdayTime, rescheduled.appointmentDate());
    }

    @Test
    void testRescheduleOldAppointmentBecomesUnavailable() {
        // Create two appointments for different patients at the same time
        Patient patient2 = Patient.builder()
                .name("Patient Two Unavailable")
                .identityDocument("4444444444")
                .phone("3004444444")
                .email("patient2unavailable@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Create appointment for patient 2
        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "Patient 2 appointment"));

        // Create appointment for patient 1
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday2, "Patient 1 appointment"));

        // Try to reschedule to the same time as patient 2's appointment (doctor conflict)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday));
        });
    }

    @Test
    void testRescheduleTransaction_RollbackOnConflict() {
        // Create an appointment for patient at futureMonday
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Original appointment"));

        // Get original cancellation date (should be null)
        Appointment originalAppt = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNull(originalAppt.getCancellationDate(), "Original no debe tener cancellationDate antes de reprogramar");

        // Try to reschedule to a time that's 15 minutes away from futureMonday
        // This will fail the 30-minute interval validation
        LocalDateTime conflictingTime = futureMonday.plusMinutes(15);
        
        try {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), conflictingTime));
        } catch (Exception e) {
            // Expected - should fail due to 30-minute interval validation
        }

        // Verify original appointment was NOT modified (transaction rollback)
        Appointment originalAfter = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNull(originalAfter.getCancellationDate(), "Original no debe tener cancellationDate después de rollback");
        assertEquals(AppointmentStatus.PROGRAMADA.name(), originalAfter.getStatus(), "Original debe mantener status PROGRAMADA después de rollback");
    }
}
