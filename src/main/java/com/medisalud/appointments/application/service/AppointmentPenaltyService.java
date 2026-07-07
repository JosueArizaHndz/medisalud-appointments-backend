package com.medisalud.appointments.application.service;

import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Penalty;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import com.medisalud.appointments.domain.port.out.PenaltyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maneja toda la lógica de penalizaciones por cancelación tardía.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentPenaltyService {

    private static final int HOURS_THRESHOLD = 2;
    private final PenaltyRepositoryPort penaltyRepositoryPort;

    /**
     * Crea una penalización si la cancelación es con menos de 2 horas de anticipación.
     *
     * @param appointment La cita que se va a cancelar
     * @param reason Descripción de la razón de la penalización
     * @return true si se creó penalización, false si no aplica
     */
    public boolean createPenaltyIfLateCancellation(Appointment appointment, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate();
        long hoursUntilAppointment = java.time.Duration.between(now, appointmentDateTime).toHours();

        if (hoursUntilAppointment < HOURS_THRESHOLD) {
            Penalty penalty = Penalty.builder()
                    .patientId(appointment.getPatientId())
                    .penaltyType("CANCELACION_TARDIA")
                    .description(reason)
                    .build();
            penaltyRepositoryPort.save(penalty);
            log.info("Penalización creada para paciente {}: {}", appointment.getPatientId(), reason);
            return true;
        }
        return false;
    }

    /**
     * Verifica si un paciente está bloqueado por penalizaciones (3 o más en los últimos 30 días).
     *
     * @param patientId ID del paciente
     * @return true si el paciente está bloqueado
     */
    public boolean isPatientBlocked(UUID patientId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentPenalties = penaltyRepositoryPort.countByPatientIdAndCreatedAtAfter(patientId, thirtyDaysAgo);
        return recentPenalties >= 3;
    }

    /**
     * Obtiene el número de penalizaciones recientes de un paciente.
     */
    public long countRecentPenalties(UUID patientId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return penaltyRepositoryPort.countByPatientIdAndCreatedAtAfter(patientId, thirtyDaysAgo);
    }
}
