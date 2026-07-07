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
        // Crear doctor de prueba
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

        // Crear paciente de prueba
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
     * Verifica que para un día lunes se retornen los 20 slots disponibles
     * (horario 08:00 a 18:00 con intervalos de 30 minutos).
     */
    @Test
    void getAvailableSlots_shouldReturnSlotsForMonday() {
        // Lunes en el futuro
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Lunes: 08:00 a 18:00, intervalos de 30 min = 20 slots
        assertEquals(20, slots.size(), "Lunes debe tener 20 slots disponibles (08:00-18:00)");
        
        // El primer slot debe ser 08:00
        assertEquals("08:00", slots.get(0).time());
        // El último slot debe ser 17:30
        assertEquals("17:30", slots.get(slots.size() - 1).time());
    }

    /**
     * Verifica que para un día sábado se retornen los 10 slots disponibles
     * (horario 08:00 a 13:00 con intervalos de 30 minutos).
     */
    @Test
    void getAvailableSlots_shouldReturnSlotsForSaturday() {
        // Sábado en el futuro
        LocalDate saturday = LocalDate.now().plusDays(9).with(java.time.DayOfWeek.SATURDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            saturday,
            saturday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Sábado: 08:00 a 13:00, intervalos de 30 min = 10 slots
        assertEquals(10, slots.size(), "Sábado debe tener 10 slots disponibles (08:00-13:00)");
    }

    /**
     * Verifica que para un día domingo no se retornen slots disponibles
     * (no hay atención los domingos).
     */
    @Test
    void getAvailableSlots_shouldReturnEmptyForSunday() {
        // Domingo en el futuro
        LocalDate sunday = LocalDate.now().plusDays(8).with(java.time.DayOfWeek.SUNDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            sunday,
            sunday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Domingo: no se permiten citas
        assertTrue(slots.isEmpty(), "Domingo no debe tener slots disponibles");
    }

    /**
     * Verifica que los slots ya reservados (con estado PROGRAMADA) no aparezcan
     * en la lista de slots disponibles.
     */
    @Test
    void getAvailableSlots_shouldExcludeBookedSlots() {
        // Lunes en el futuro
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDateTime bookedTime = monday.atTime(10, 0);

        // Crear una cita a las 10:00
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(bookedTime)
                .status(AppointmentStatus.PROGRAMADA)
                .build();
        appointmentRepositoryPort.save(appointment);

        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Debe tener 19 slots (20 - 1 reservado)
        assertEquals(19, slots.size(), "Debe excluir el slot reservado");

        // Verificar que 10:00 no está en los slots disponibles
        boolean has10AM = slots.stream()
                .anyMatch(slot -> "10:00".equals(slot.time()));
        assertFalse(has10AM, "10:00 no debe estar disponible");
    }

    /**
     * Verifica que las citas canceladas no bloquean el slot, permitiendo
     * que el horario vuelva a estar disponible para nuevas reservas.
     */
    @Test
    void getAvailableSlots_shouldIncludeCancelledAppointments() {
        // Lunes en el futuro
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDateTime cancelledTime = monday.atTime(10, 0);

        // Crear una cita cancelada a las 10:00
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentDate(cancelledTime)
                .status(AppointmentStatus.CANCELADA)
                .build();
        appointmentRepositoryPort.save(appointment);

        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Debe tener 20 slots (cancelada no bloquea)
        assertEquals(20, slots.size(), "Las citas canceladas no deben bloquear slots");

        // Verificar que 10:00 SÍ está en los slots disponibles
        boolean has10AM = slots.stream()
                .anyMatch(slot -> "10:00".equals(slot.time()));
        assertTrue(has10AM, "10:00 debe estar disponible para cita cancelada");
    }

    /**
     * Verifica que al consultar slots para un rango de múltiples días,
     * se retornen correctamente todos los slots acumulados.
     */
    @Test
    void getAvailableSlots_shouldWorkForMultipleDays() {
        // Rango de lunes a miércoles
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        LocalDate wednesday = monday.plusDays(2);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            wednesday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Lunes: 20 slots, Martes: 20 slots, Miércoles: 20 slots = 60 slots
        assertEquals(60, slots.size(), "3 días laborables deben tener 60 slots");
    }

    /**
     * Verifica que al consultar slots para un rango de fechas que incluye domingo,
     * se omitan los slots del domingo en el resultado.
     */
    @Test
    void getAvailableSlots_shouldSkipSundayInRange() {
        // Rango de viernes a lunes (incluye domingo)
        LocalDate friday = LocalDate.now().plusDays(6).with(java.time.DayOfWeek.FRIDAY);
        LocalDate nextMonday = friday.plusDays(3);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            friday,
            nextMonday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);

        // Viernes: 20 slots, Sábado: 10 slots, Domingo: 0 slots, Lunes: 20 slots = 50 slots
        assertEquals(50, slots.size(), "Debe saltar el domingo");
    }

    /**
     * Verifica que cada slot disponible tenga la estructura correcta con datetime,
     * fecha, hora y día de la semana.
     */
    @Test
    void availableSlot_shouldHaveCorrectStructure() {
        LocalDate monday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY);
        
        AvailableSlotsQuery query = new AvailableSlotsQuery(
            doctorId,
            monday,
            monday
        );

        List<AvailableSlot> slots = appointmentService.getAvailableSlots(query);
        
        assertFalse(slots.isEmpty(), "Debe tener al menos un slot");
        
        AvailableSlot firstSlot = slots.get(0);
        
        assertNotNull(firstSlot.slot(), "El slot debe tener datetime");
        assertEquals(monday, firstSlot.date(), "La fecha del slot debe coincidir");
        assertNotNull(firstSlot.time(), "El slot debe tener hora");
        assertEquals("MONDAY", firstSlot.dayOfWeek(), "El slot debe tener día de la semana");
    }
}
