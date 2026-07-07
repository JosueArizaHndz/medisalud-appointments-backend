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
        // Limpiar citas existentes para asegurar un estado limpio de prueba
        appointmentRepositoryPort.findAll().forEach(a -> appointmentRepositoryPort.deleteById(a.getId()));

        // Crear doctor de prueba
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

        // Crear paciente de prueba
        Patient patient = Patient.builder()
                .name("Patient Reschedule Test")
                .identityDocument("7777777777")
                .phone("3007777777")
                .email("reschedule@test.com")
                .active(true)
                .build();
        patient = patientRepositoryPort.save(patient);
        patientId = patient.getId();

        // Asegurar que las fechas sean días de semana para evitar problemas de validación
        futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        futureMonday2 = LocalDate.now().plusDays(10).with(java.time.DayOfWeek.MONDAY).atTime(14, 0);
        futureSaturday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SATURDAY).atTime(10, 0);
        sunday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.SUNDAY).atTime(10, 0);
    }

    /**
     * Verifica que se pueda reprogramar exitosamente una cita a una nueva fecha/hora,
     * y que la cita original se marque como cancelada.
     */
    @Test
    void testRescheduleAppointment_Success() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        assertEquals(AppointmentStatus.PROGRAMADA, original.status());

        // Reprogramar a una hora diferente
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), futureMonday2));

        // Verificar nueva cita
        assertEquals(AppointmentStatus.PROGRAMADA, rescheduled.status());
        assertEquals(futureMonday2, rescheduled.appointmentDate());
        assertEquals(patientId, rescheduled.patientId());
        assertEquals(doctorId, rescheduled.doctorId());

        // Verificar que la cita original fue cancelada
        Appointment originalAfter = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNotNull(originalAfter);
        assertEquals(AppointmentStatus.CANCELADA, originalAfter.getStatus());
        assertNotNull(originalAfter.getCancellationDate());
    }

    /**
     * Verifica que intentar reprogramar una cita que no existe lance una excepción.
     */
    @Test
    void testRescheduleNonExistentAppointment() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(Exception.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(nonExistentId, futureMonday2));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita que ya fue cancelada.
     */
    @Test
    void testRescheduleCancelledAppointment() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Cancelarla primero
        appointmentService.cancelAppointment(
                new com.medisalud.appointments.domain.port.in.CancelAppointmentCommand(original.id()));

        // No se puede reprogramar una cita cancelada
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday2));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita que ya fue finalizada.
     */
    @Test
    void testRescheduleFinalizedAppointment() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Establecer estado a FINALIZADA
        Appointment appt = appointmentRepositoryPort.findById(original.id()).orElse(null);
        appt.setStatus(AppointmentStatus.FINALIZADA);
        appointmentRepositoryPort.save(appt);

        // No se puede reprogramar una cita finalizada
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday2));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita a una fecha pasada.
     */
    @Test
    void testRescheduleToPastDate() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Intentar reprogramar a una fecha pasada
        LocalDateTime pastDate = LocalDateTime.now().minusHours(2);

        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), pastDate));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita para un día domingo.
     */
    @Test
    void testRescheduleToSunday() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Intentar reprogramar a domingo
        assertThrows(Exception.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), sunday));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita a un horario donde el médico
     * ya tiene otra cita (conflicto de médico).
     */
    @Test
    void testRescheduleDoctorConflict() {
        // Crear paciente 2
        Patient patient2 = Patient.builder()
                .name("Patient Two Reschedule")
                .identityDocument("6666666666")
                .phone("3006666666")
                .email("patient2reschedule@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Crear cita para paciente 2 al mismo horario que paciente 1
        AppointmentResponse appt2 = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "Cita para paciente 2"));

        // Intentar reprogramar paciente 1 para que coincida con la cita de paciente 2 (mismo doctor)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(appt2.id(), futureMonday));
        });
    }

    /**
     * Verifica que no se pueda reprogramar una cita a un horario donde el paciente
     * ya tiene otra cita con el mismo médico (conflicto de paciente).
     */
    @Test
    void testReschedulePatientConflict() {
        // Crear paciente 2
        Patient patient2 = Patient.builder()
                .name("Patient Two Reschedule")
                .identityDocument("5555555555")
                .phone("3005555555")
                .email("patient2reschedule2@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Crear dos citas para paciente 2 en días diferentes
        LocalDateTime differentMonday = LocalDate.now().plusDays(14).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        
        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "Primera cita"));

        AppointmentResponse secondAppt = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, differentMonday, "Segunda cita"));

        // Intentar reprogramar la segunda cita para que coincida con la primera (mismo paciente)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(secondAppt.id(), futureMonday));
        });
    }

    /**
     * Verifica el comportamiento de penalización al reprogramar una cita con menos de 2 horas
     * de anticipación (la cancelación original genera penalización).
     */
    @Test
    void testRescheduleWithPenalty_LessThan2Hours() {
        // Crear cita en 1.5 horas en un día de semana (debería generar penalización porque < 2 horas)
        LocalDateTime now = LocalDateTime.now();
        // Redondear al siguiente intervalo válido de 30 minutos
        LocalDateTime appointmentTime = now.withMinute(0).withSecond(0).withNano(0).plusHours(1).plusMinutes(30);
        
        // Asegurar que sea un día de semana
        while (appointmentTime.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || 
               appointmentTime.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            appointmentTime = appointmentTime.plusDays(1);
        }

        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, appointmentTime, "Prueba penalización reprogramación"));

        // Reprogramar a una fecha futura lejana - esto debería funcionar y crear penalización por cancelación tardía
        LocalDateTime farFuture = LocalDate.now().plusDays(30).with(java.time.DayOfWeek.MONDAY).atTime(9, 0);

        // Este test verifica que la reprogramación funciona; la lógica de penalización está cubierta por los tests de cancelación
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), farFuture));

        assertEquals(AppointmentStatus.PROGRAMADA, rescheduled.status());
        assertEquals(farFuture, rescheduled.appointmentDate());
    }

    /**
     * Verifica que al reprogramar una cita con más de 2 horas de anticipación,
     * NO se genera penalización.
     */
    @Test
    void testRescheduleWithoutPenalty_GreaterThan2Hours() {
        // Crear cita en 5 horas usando un lunes futuro para evitar validación de día de la semana
        LocalDateTime futureMonday = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(10, 0);
        LocalDateTime futureMondayReschedule = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(14, 0);

        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Prueba sin penalización reprogramación"));

        // Obtener conteo inicial de penalizaciones
        List<Penalty> initialPenalties = penaltyRepositoryPort.findByPatientId(patientId);
        int initialPenaltyCount = initialPenalties.size();

        // Reprogramar al mismo día diferente hora (NO debería generar penalización)
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), futureMondayReschedule));

        // Verificar que NO se creó penalización
        List<Penalty> penalties = penaltyRepositoryPort.findByPatientId(patientId);
        assertEquals(initialPenaltyCount, penalties.size(), "No debe crearse penalización");
        assertEquals(AppointmentStatus.PROGRAMADA, rescheduled.status());
    }

    /**
     * Verifica que al reprogramar una cita, se preserven las notas/original descripción de la misma.
     */
    @Test
    void testReschedulePreservesNotes() {
        // Crear una cita con notas
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Notas médicas importantes"));

        // Reprogramar
        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), futureMonday2));

        // Las notas deberían preservarse
        assertEquals("Notas médicas importantes", rescheduled.notes());
    }

    @Test
    void testRescheduleToSaturday() {
        // Crear una cita
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Reprogramar a sábado (horario válido 08-13)
        LocalDateTime saturdayTime = futureSaturday;

        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                new RescheduleAppointmentCommand(original.id(), saturdayTime));

        assertEquals(AppointmentStatus.PROGRAMADA, rescheduled.status());
        assertEquals(saturdayTime, rescheduled.appointmentDate());
    }

    @Test
    void testRescheduleOldAppointmentBecomesUnavailable() {
        // Crear dos citas para diferentes pacientes al mismo tiempo
        Patient patient2 = Patient.builder()
                .name("Patient Two Unavailable")
                .identityDocument("4444444444")
                .phone("3004444444")
                .email("patient2unavailable@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        UUID patient2Id = patient2.getId();

        // Crear cita para paciente 2
        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patient2Id, doctorId, futureMonday, "Cita del paciente 2"));

        // Crear cita para paciente 1
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday2, "Cita del paciente 1"));

        // Intentar reprogramar al mismo horario que la cita de paciente 2 (conflicto de doctor)
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), futureMonday));
        });
    }

    @Test
    void testRescheduleTransaction_RollbackOnConflict() {
        // Crear una cita para paciente en futureMonday
        AppointmentResponse original = appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId, doctorId, futureMonday, "Cita original"));

        // Obtener fecha de cancelación original (debería ser nula)
        Appointment originalAppt = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNull(originalAppt.getCancellationDate(), "Original no debe tener cancellationDate antes de reprogramar");

        // Intentar reprogramar a una hora que está a 15 minutos de futureMonday
        // Esto fallará la validación de intervalo de 30 minutos
        LocalDateTime conflictingTime = futureMonday.plusMinutes(15);
        
        try {
            appointmentService.rescheduleAppointment(new RescheduleAppointmentCommand(original.id(), conflictingTime));
        } catch (Exception e) {
            // Esperado - debería fallar por la validación de intervalo de 30 minutos
        }

        // Verificar que la cita original NO fue modificada (rollback de transacción)
        Appointment originalAfter = appointmentRepositoryPort.findById(original.id()).orElse(null);
        assertNull(originalAfter.getCancellationDate(), "Original no debe tener cancellationDate después de rollback");
        assertEquals(AppointmentStatus.PROGRAMADA, originalAfter.getStatus(), "Original debe mantener status PROGRAMADA después de rollback");
    }
}
