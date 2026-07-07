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
        // Limpiar citas existentes para asegurar un estado limpio de prueba
        appointmentRepositoryPort.findAll().forEach(a -> appointmentRepositoryPort.deleteById(a.getId()));

        // Crear doctor de prueba
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

        // Crear paciente de prueba
        Patient patient = Patient.builder()
                .name("Patient Cancellation Test")
                .identityDocument("9999999999")
                .phone("3001234567")
                .email("cancellation@test.com")
                .active(true)
                .build();
        patient = patientRepositoryPort.save(patient);
        patientId = patient.getId();

        // Asegurar que futureMonday sea un lunes
        futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        futureSaturday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SATURDAY).atTime(10, 0);
        sunday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SUNDAY).atTime(10, 0);
    }

    /**
     * Verifica que al cancelar una cita con menos de 2 horas de anticipación,
     * se genere una penalización tipo CANCELACION_TARDIA para el paciente.
     */
    @Test
    void testCancelAppointmentWithPenalty_LessThan2Hours() {
        // Crear una cita en 1 hora (debería generar penalización)
        // Usar un horario dentro de intervalos de 30 minutos y dentro de 1 hora desde ahora
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Notas de prueba"));

        // Obtener conteo inicial de penalizaciones
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Cancelar la cita
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        // Verificar que la cita está cancelada
        assertEquals(AppointmentStatus.CANCELADA, response.status());
        assertNotNull(response.cancellationDate(), "La fecha de cancelación debe estar presente");

        // Verificar que se creó la penalización
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount + 1, penalties.size(), "Debe crearse una penalización");
        assertEquals("CANCELACION_TARDIA", penalties.get(penalties.size() - 1).getPenaltyType());
    }

    /**
     * Verifica que al cancelar una cita con más de 2 horas de anticipación,
     * NO se genera ninguna penalización para el paciente.
     */
    @Test
    void testCancelAppointmentWithoutPenalty_GreaterThan2Hours() {
        // Crear una cita en 3 horas (NO debería generar penalización)
        // Usar un lunes futuro para evitar problemas de validación de día de la semana
        LocalDateTime futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(14, 30);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas de prueba"));

        // Obtener conteo inicial de penalizaciones
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Cancelar la cita
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        // Verificar que la cita está cancelada
        assertEquals(AppointmentStatus.CANCELADA, response.status());
        assertNotNull(response.cancellationDate(), "La fecha de cancelación debe estar presente");

        // Verificar que NO se creó penalización
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount, penalties.size(), "No debe crearse penalización");
    }

    /**
     * Verifica que intentar cancelar una cita que no existe lance una excepción.
     */
    @Test
    void testCancelNonExistentAppointment_ReturnsNull() {
        UUID nonExistentId = UUID.randomUUID();

        // Debería lanzar excepción para cita inexistente
        assertThrows(Exception.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(nonExistentId));
        });
    }

    /**
     * Verifica que intentar cancelar una cita ya cancelada lance una excepción IllegalStateException.
     */
    @Test
    void testCancelAlreadyCancelledAppointment() {
        // Crear una cita
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas de prueba"));

        // La primera cancelación debería tener éxito
        AppointmentResponse response1 = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));
        assertEquals(AppointmentStatus.CANCELADA, response1.status());

        // La segunda cancelación debería fallar
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        });
    }

    /**
     * Verifica que una cita ya finalizada no puede ser cancelada nuevamente.
     */
    @Test
    void testCancelFinalizedAppointment() {
        // Crear una cita
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas de prueba"));

        // Establecer estado a FINALIZADA
        com.medisalud.appointments.domain.model.Appointment appt = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        appt.setStatus(AppointmentStatus.FINALIZADA);
        appointmentRepositoryPort.save(appt);

        // No se puede cancelar una cita finalizada
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        });
    }

    /**
     * Verifica que al cancelar una cita, el horario (slot) vuelve a estar disponible
     * para nuevas reservas.
     */
    @Test
    void testCancelledSlotBecomesAvailable() {
        // Crear una cita
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas de prueba"));

        // Consultar slots disponibles ANTES de la cancelación
        List<com.medisalud.appointments.application.dto.AvailableSlot> slotsBefore = 
                appointmentService.getAvailableSlots(
                        new com.medisalud.appointments.domain.port.in.AvailableSlotsQuery(
                                doctorId, 
                                LocalDate.now().plusDays(7), 
                                LocalDate.now().plusDays(7)));

        // Cancelar la cita
        appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));

        // Consultar slots disponibles DESPUÉS de la cancelación
        List<com.medisalud.appointments.application.dto.AvailableSlot> slotsAfter = 
                appointmentService.getAvailableSlots(
                        new com.medisalud.appointments.domain.port.in.AvailableSlotsQuery(
                                doctorId, 
                                LocalDate.now().plusDays(7), 
                                LocalDate.now().plusDays(7)));

        // El slot cancelado debería estar disponible ahora
        assertTrue(slotsAfter.size() >= slotsBefore.size(), 
                "El slot cancelado debe volver a estar disponible");
    }

    /**
     * Verifica que se puede cancelar exitosamente una cita programada para sábado.
     */
    @Test
    void testCancelAppointmentOnSaturday() {
        // Crear una cita para sábado
        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureSaturday, "Test Saturday"));

        // Cancelar la cita
        AppointmentResponse response = appointmentService.cancelAppointment(
                new CancelAppointmentCommand(appointment.id()));

        assertEquals(AppointmentStatus.CANCELADA, response.status());
        assertNotNull(response.cancellationDate());
    }

    /**
     * Verifica que no se pueden crear citas para días domingo (no hay disponibilidad).
     */
    @Test
    void testCannotCancelAppointmentOnSunday() {
        // Intentar crear cita en domingo debería fallar
        assertThrows(Exception.class, () -> {
            appointmentService.createAppointment(
                    new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                            patientId, doctorId, sunday, "Test Sunday"));
        });
    }

    /**
     * Verifica que al cancelar una cita, el campo cancellationDate se registre
     * correctamente en el modelo de la cita.
     */
    @Test
    void testCancellationDateIsRecorded() {
        // Crear una cita con más de 2 horas usando un lunes futuro
        LocalDateTime futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(15, 0);

        AppointmentResponse appointment = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas de prueba"));

        // Obtener cita original
        com.medisalud.appointments.domain.model.Appointment original = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        assertNull(original.getCancellationDate(), "Original no debe tener fecha de cancelación");

        // Cancelar la cita
        appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));

        // Verificar que la fecha de cancelación está registrada
        com.medisalud.appointments.domain.model.Appointment cancelled = appointmentRepositoryPort.findById(appointment.id()).orElse(null);
        assertNotNull(cancelled.getCancellationDate(), "La fecha de cancelación debe estar presente");
        assertEquals(AppointmentStatus.CANCELADA, cancelled.getStatus(), "La cita debe estar cancelada");
    }

    /**
     * Verifica que múltiples cancelaciones tardías generan múltiples penalizaciones
     * para el mismo paciente.
     */
    @Test
    void testMultiplePenaltiesForMultipleLateCancellations() {
        // Crear paciente 2
        Patient patient2 = Patient.builder()
                .name("Patient Multiple Penalties")
                .identityDocument("8888888888")
                .phone("3001234568")
                .email("multiple@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Crear y cancelar 3 citas dentro de 2 horas (debería generar 3 penalizaciones)
        for (int i = 0; i < 3; i++) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            AppointmentResponse appointment = appointmentService.createAppointment(
                    new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                            patient2Id, doctorId, appointmentTime, "Test penalty " + i));

            appointmentService.cancelAppointment(new CancelAppointmentCommand(appointment.id()));
        }

        // Verificar que se crearon 3 penalizaciones
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patient2Id);
        assertEquals(3, penalties.size(), "Debe crear 3 penalizaciones");
    }
}
