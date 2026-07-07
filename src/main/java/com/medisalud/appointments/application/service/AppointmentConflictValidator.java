package com.medisalud.appointments.application.service;

import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Valida conflictos de citas para médicos y pacientes.
 * Extrae la lógica de verificación de superposición de horarios.
 */
@Component
@RequiredArgsConstructor
public class AppointmentConflictValidator {

    private final AppointmentRepositoryPort appointmentRepositoryPort;

    /**
     * Valida que el médico no tenga una cita conflictiva en el horario solicitado.
     * Lanza IllegalStateException si hay conflicto.
     *
     * @param doctorId ID del médico a validar
     * @param appointmentDate Fecha y hora de la cita a verificar
     * @param excludeAppointmentId ID de cita a excluir (null para nueva cita, ID para reprogramación)
     */
    public void validateDoctorConflict(UUID doctorId, LocalDateTime appointmentDate, UUID excludeAppointmentId) {
        LocalDateTime dayStart = appointmentDate.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = appointmentDate.withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        for (Appointment appt : allAppointments) {
            if (appt.getDoctorId().equals(doctorId)
                    && !AppointmentStatus.CANCELADA.equals(appt.getStatus())
                    && (excludeAppointmentId == null || !appt.getId().equals(excludeAppointmentId))) {
                long minutesDiff = Duration.between(appt.getAppointmentDate(), appointmentDate).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException("El médico ya tiene una cita en el horario seleccionado");
                }
            }
        }
    }

    /**
     * Valida que el paciente no tenga una cita conflictiva con el MISMO médico.
     * Lanza IllegalStateException si hay conflicto.
     *
     * @param patientId ID del paciente a validar
     * @param doctorId ID del médico
     * @param appointmentDate Fecha y hora de la cita a verificar
     * @param excludeAppointmentId ID de cita a excluir (null para nueva cita, ID para reprogramación)
     */
    public void validatePatientDoctorConflict(UUID patientId, UUID doctorId, LocalDateTime appointmentDate, UUID excludeAppointmentId) {
        LocalDateTime dayStart = appointmentDate.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = appointmentDate.withHour(23).withMinute(59).withSecond(59);
        List<Appointment> allAppointments = appointmentRepositoryPort.findByAppointmentDateBetween(dayStart, dayEnd);

        for (Appointment appt : allAppointments) {
            if (appt.getPatientId().equals(patientId)
                    && appt.getDoctorId().equals(doctorId)
                    && !AppointmentStatus.CANCELADA.equals(appt.getStatus())
                    && (excludeAppointmentId == null || !appt.getId().equals(excludeAppointmentId))) {
                long minutesDiff = Duration.between(appt.getAppointmentDate(), appointmentDate).toMinutes();
                if (Math.abs(minutesDiff) < 30) {
                    throw new IllegalStateException("El paciente ya tiene una cita con este médico en el horario seleccionado");
                }
            }
        }
    }
}
