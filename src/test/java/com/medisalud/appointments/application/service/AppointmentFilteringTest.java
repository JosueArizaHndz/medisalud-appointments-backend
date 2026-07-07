package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.ListAppointmentsQuery;
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
class AppointmentFilteringTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepositoryPort doctorRepositoryPort;

    @Autowired
    private PatientRepositoryPort patientRepositoryPort;

    @Autowired
    private AppointmentRepositoryPort appointmentRepositoryPort;

    private UUID doctorId1;
    private UUID doctorId2;
    private UUID patientId1;
    private UUID patientId2;
    private LocalDateTime mondayMorning;
    private LocalDateTime mondayAfternoon;
    private LocalDateTime tuesdayMorning;

    @BeforeEach
    void setUp() {
        // Crear doctores de prueba
        Doctor doctor1 = Doctor.builder()
                .name("Dr. Filter Test 1")
                .email("filter.doctor1@medisalud.com")
                .specialty("GENERAL")
                .licenseNumber("TESTFILTER001")
                .maxPatients(50)
                .active(true)
                .build();
        doctor1 = doctorRepositoryPort.save(doctor1);
        doctorId1 = doctor1.getId();

        Doctor doctor2 = Doctor.builder()
                .name("Dr. Filter Test 2")
                .email("filter.doctor2@medisalud.com")
                .specialty("CARDIOLOGY")
                .licenseNumber("TESTFILTER002")
                .maxPatients(50)
                .active(true)
                .build();
        doctor2 = doctorRepositoryPort.save(doctor2);
        doctorId2 = doctor2.getId();

        // Crear pacientes de prueba
        Patient patient1 = Patient.builder()
                .name("Patient Filter Test 1")
                .identityDocument("1111111111")
                .phone("3001111111")
                .email("filter.patient1@test.com")
                .active(true)
                .build();
        patient1 = patientRepositoryPort.save(patient1);
        patientId1 = patient1.getId();

        Patient patient2 = Patient.builder()
                .name("Patient Filter Test 2")
                .identityDocument("2222222222")
                .phone("3002222222")
                .email("filter.patient2@test.com")
                .active(true)
                .build();
        patient2 = patientRepositoryPort.save(patient2);
        patientId2 = patient2.getId();

        // Crear fechas de citas
        mondayMorning = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(9, 0);
        mondayAfternoon = LocalDate.now().plusDays(7).with(java.time.DayOfWeek.MONDAY).atTime(14, 0);
        tuesdayMorning = LocalDate.now().plusDays(8).with(java.time.DayOfWeek.TUESDAY).atTime(10, 0);

        // Crear citas
        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId1, doctorId1, mondayMorning, "Appointment 1"));

        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId1, doctorId1, mondayAfternoon, "Appointment 2"));

        appointmentService.createAppointment(
                new com.medisalud.appointments.domain.port.in.CreateAppointmentCommand(
                        patientId2, doctorId2, tuesdayMorning, "Appointment 3"));
    }

    /**
     * Verifica que al listar todas las citas sin filtros, se retornen todas las citas existentes.
     */
    @Test
    void testGetAllAppointments_NoFilters() {
        List<AppointmentResponse> all = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, null, null));

        assertEquals(3, all.size(), "Debe retornar todas las citas");
    }

    /**
     * Verifica que el filtro por ID de doctor funcione correctamente,
     * retornando solo las citas de ese médico.
     */
    @Test
    void testFilterByDoctorId() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(doctorId1, null, null, null, null));

        assertEquals(2, filtered.size(), "Debe filtrar por doctor");
        filtered.forEach(r -> assertEquals(doctorId1, r.doctorId()));
    }

    /**
     * Verifica que el filtro por ID de paciente funcione correctamente,
     * retornando solo las citas de ese paciente.
     */
    @Test
    void testFilterByPatientId() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, patientId1, null, null, null));

        assertEquals(2, filtered.size(), "Debe filtrar por paciente");
        filtered.forEach(r -> assertEquals(patientId1, r.patientId()));
    }

    /**
     * Verifica que el filtro por estado de cita funcione correctamente,
     * retornando solo las citas con el estado especificado.
     */
    @Test
    void testFilterByStatus() {
        List<AppointmentResponse> all = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, null, null));

        // Todas deberían ser PROGRAMADA
        List<AppointmentResponse> programadas = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, "PROGRAMADA", null, null));

        assertEquals(all.size(), programadas.size(), "Todas las citas deben ser PROGRAMADA");
    }

    /**
     * Verifica que el filtro por fecha de inicio funcione correctamente,
     * retornando solo las citas a partir de la fecha especificada.
     */
    @Test
    void testFilterByStartDate() {
        // Filtrar desde mondayAfternoon hacia adelante - debe obtener mondayAfternoon y tuesdayMorning
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, mondayAfternoon, null));

        assertEquals(2, filtered.size(), "Debe filtrar por fecha inicio");
    }

    /**
     * Verifica que el filtro por fecha de fin funcione correctamente,
     * retornando solo las citas hasta la fecha especificada.
     */
    @Test
    void testFilterByEndDate() {
        // Filtrar hasta mondayMorning
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, null, mondayMorning));

        assertEquals(1, filtered.size(), "Debe filtrar por fecha fin");
    }

    /**
     * Verifica que el filtro por rango de fechas (inicio y fin) funcione correctamente.
     */
    @Test
    void testFilterByDateRange() {
        LocalDateTime start = mondayMorning.minusHours(1);
        LocalDateTime end = mondayAfternoon.plusHours(1);

        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, start, end));

        assertEquals(2, filtered.size(), "Debe filtrar por rango de fechas");
    }

    /**
     * Verifica que se puedan combinar múltiples filtros (doctor + estado) correctamente.
     */
    @Test
    void testFilterByDoctorAndStatus() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(doctorId1, null, "PROGRAMADA", null, null));

        assertEquals(2, filtered.size(), "Debe filtrar por doctor y estado");
    }

    /**
     * Verifica que se puedan combinar filtros de paciente con rango de fechas.
     */
    @Test
    void testFilterByPatientAndDateRange() {
        LocalDateTime start = mondayMorning.minusHours(1);
        LocalDateTime end = mondayMorning.plusHours(1);

        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, patientId1, null, start, end));

        assertEquals(1, filtered.size(), "Debe filtrar por paciente y rango de fechas");
    }

    /**
     * Verifica que al filtrar con un ID que no existe, se retorne una lista vacía.
     */
    @Test
    void testFilterNoResults_ReturnsEmptyList() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(UUID.randomUUID(), null, null, null, null));

        assertTrue(filtered.isEmpty(), "Debe retornar lista vacía cuando no hay resultados");
    }

    /**
     * Verifica que al filtrar con un estado inválido, se retorne una lista vacía.
     */
    @Test
    void testFilterByNonExistentStatus() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, "INVALID_STATUS", null, null));

        assertTrue(filtered.isEmpty(), "Debe retornar lista vacía para estado inválido");
    }

    /**
     * Verifica que se puedan combinar todos los parámetros de filtro simultáneamente
     * (doctor, paciente, estado) correctamente.
     */
    @Test
    void testFilterCombination_AllParameters() {
        List<AppointmentResponse> filtered = appointmentService.listAppointments(
                new ListAppointmentsQuery(doctorId2, patientId2, "PROGRAMADA", null, null));

        assertEquals(1, filtered.size(), "Debe filtrar por múltiples parámetros");
        AppointmentResponse result = filtered.get(0);
        assertEquals(doctorId2, result.doctorId());
        assertEquals(patientId2, result.patientId());
        assertEquals(AppointmentStatus.PROGRAMADA, result.status());
    }

    /**
     * Verifica que las citas no canceladas tengan el campo cancellationDate en null.
     */
    @Test
    void testResponseContainsCancellationDate() {
        List<AppointmentResponse> all = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, null, null));

        // Todas deberían tener cancellationDate en null (no canceladas)
        all.forEach(r -> assertNull(r.cancellationDate(), "Citas no canceladas no deben tener cancellationDate"));
    }

    /**
     * Verifica que la respuesta de cada cita contenga toda la información relevante:
     * ID, paciente, doctor, especialidad, fecha, estado, etc.
     */
    @Test
    void testResponseContainsRelevantInfo() {
        List<AppointmentResponse> all = appointmentService.listAppointments(
                new ListAppointmentsQuery(null, null, null, null, null));

        all.forEach(r -> {
            assertNotNull(r.id(), "Debe tener ID");
            assertNotNull(r.patientId(), "Debe tener patientId");
            assertNotNull(r.patientName(), "Debe tener patientName");
            assertNotNull(r.doctorId(), "Debe tener doctorId");
            assertNotNull(r.doctorName(), "Debe tener doctorName");
            assertNotNull(r.doctorSpecialty(), "Debe tener doctorSpecialty");
            assertNotNull(r.appointmentDate(), "Debe tener appointmentDate");
            assertNotNull(r.status(), "Debe tener status");
        });
    }
}
