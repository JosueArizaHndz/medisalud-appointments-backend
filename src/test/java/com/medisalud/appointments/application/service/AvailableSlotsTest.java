package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.AvailableSlotsQuery;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AvailableSlotsTest {

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

        // Create test patient
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

    @Test
    void getAvailableSlots_shouldReturnSlotsForMonday() {
        // Monday in the future
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Monday: 08:00 to 18:00, 30-min intervals = 20 slots
        assertEquals(20, slots.size(), "Monday should have 20 available slots (08:00-18:00)");
        
        // First slot should be 08:00
        assertEquals("08:00", slots.get(0).time());
        // Last slot should be 17:30
        assertEquals("17:30", slots.get(slots.size() - 1).time());
    }

    @Test
    void getAvailableSlots_shouldReturnSlotsForSaturday() {
        // Saturday in the future
        LocalDate saturday = LocalDate.now().plusDays(9).with(java.time.DayOfWeek.SATURDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            saturday,
            saturday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Saturday: 08:00 to 13:00, 30-min intervals = 10 slots
        assertEquals(10, slots.size(), "Saturday should have 10 available slots (08:00-13:00)");
    }

    @Test
    void getAvailableSlots_shouldReturnEmptyForSunday() {
        // Sunday in the future
        LocalDate sunday = LocalDate.now().plusDays(8).with(java.time.DayOfWeek.SUNDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            sunday,
            sunday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Sunday: no appointments allowed
        assertTrue(slots.isEmpty(), "Sunday should have no available slots");
    }

    @Test
    void getAvailableSlots_shouldExcludeBookedSlots() {
        // Monday in the future
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDateTime bookedTime = monday.atTime(10, 0);

        // Create an appointment at 10:00
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(bookedTime)
                .status(AppointmentStatus.PROGRAMADA.name())
                .build();
        appointmentRepositoryPort.save(appointment);

        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Should have 19 slots (20 - 1 booked)
        assertEquals(19, slots.size(), "Should exclude booked slot");

        // Verify 10:00 is not in the available slots
        boolean has10AM = slots.stream()
                .anyMatch(slot -> "10:00".equals(slot.time()));
        assertFalse(has10AM, "10:00 should not be available");
    }

    @Test
    void getAvailableSlots_shouldIncludeCancelledAppointments() {
        // Monday in the future
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDateTime cancelledTime = monday.atTime(10, 0);

        // Create a cancelled appointment at 10:00
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(cancelledTime)
                .status(AppointmentStatus.CANCELADA.name())
                .build();
        appointmentRepositoryPort.save(appointment);

        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Should have 20 slots (cancelled doesn't block)
        assertEquals(20, slots.size(), "Cancelled appointments should not block slots");

        // Verify 10:00 IS in the available slots
        boolean has10AM = slots.stream()
                .anyMatch(slot -> "10:00".equals(slot.time()));
        assertTrue(has10AM, "10:00 should be available for cancelled appointment");
    }

    @Test
    void getAvailableSlots_shouldWorkForMultipleDays() {
        // Range from Monday to Wednesday
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDate wednesday = monday.plusDays(2);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            wednesday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Monday: 20 slots, Tuesday: 20 slots, Wednesday: 20 slots = 60 slots
        assertEquals(60, slots.size(), "3 weekdays should have 60 slots");
    }

    @Test
    void getAvailableSlots_shouldSkipSundayInRange() {
        // Range from Friday to Monday (includes Sunday)
        LocalDate friday = LocalDate.now().plusDays(6).with(java.time.DayOfWeek.FRIDAY);
        LocalDate nextMonday = friday.plusDays(3);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            friday,
            nextMonday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Friday: 20 slots, Saturday: 10 slots, Sunday: 0 slots, Monday: 20 slots = 50 slots
        assertEquals(50, slots.size(), "Should skip Sunday");
    }

    @Test
    void availableSlot_shouldHaveCorrectStructure() {
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);
        
        assertFalse(slots.isEmpty(), "Should have at least one slot");
        
        AvailableSlot firstSlot = slots.get(0);
        
        assertNotNull(firstSlot.slot(), "Slot should have datetime");
        assertEquals(monday, firstSlot.date(), "Slot date should match");
        assertNotNull(firstSlot.time(), "Slot should have time string");
        assertEquals("MONDAY", firstSlot.dayOfWeek(), "Slot should have day of week");
    }
}
