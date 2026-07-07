package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.domain.enums.AppointmentStatus;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.port.in.AvailableSlotsQuery;
import com.medisalud.appointments.domain.port.out.AppointmentRepositoryPort;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maneja la validación de horarios y la consulta de disponibilidad de médicos.
 */
@Component
@RequiredArgsConstructor
public class AppointmentAvailabilityService {

    private final AppointmentRepositoryPort appointmentRepositoryPort;
    private final DoctorRepositoryPort doctorRepositoryPort;

    /**
     * Valida que la fecha y hora de la cita cumplan con los horarios establecidos.
     *
     * @param appointmentDate Fecha y hora a validar
     * @throws IllegalArgumentException si la fecha no cumple los horarios
     */
    public void validateAppointmentHours(LocalDateTime appointmentDate) {
        java.time.DayOfWeek dayOfWeek = appointmentDate.getDayOfWeek();
        int hour = appointmentDate.getHour();
        int minute = appointmentDate.getMinute();

        // Validar intervalos de 30 minutos
        if (minute % 30 != 0) {
            throw new IllegalArgumentException("La hora debe estar en intervalos de 30 minutos (ej: 08:00, 08:30, 09:00)");
        }

        // Domingo no permitido
        if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No se pueden programar citas los domingos");
        }

        // Lunes a viernes: 08:00 a 18:00
        if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            if (hour < 8 || hour >= 18) {
                throw new IllegalArgumentException("El horario de atención de lunes a viernes es de 08:00 a 18:00");
            }
            return;
        }

        // Sábado: 08:00 a 13:00
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
            if (hour < 8 || hour >= 13) {
                throw new IllegalArgumentException("El horario de atención el sábado es de 08:00 a 13:00");
            }
        }
    }

    /**
     * Obtiene los slots disponibles para un médico en un rango de fechas.
     *
     * @param query Query con doctorId, fechaInicio y fechaFin
     * @return Lista de slots disponibles
     */
    public List<AvailableSlot> getAvailableSlots(AvailableSlotsQuery query) {
        // Validar que el médico exista
        Doctor doctor = doctorRepositoryPort.findById(query.doctorId())
                .orElseThrow(() -> new com.medisalud.appointments.infrastructure.exception.ResourceNotFoundException(
                        "Médico no encontrado con id: " + query.doctorId()));

        List<AvailableSlot> availableSlots = new ArrayList<>();

        // Iterar por cada día en el rango
        for (LocalDate date = query.fechaInicio(); !date.isAfter(query.fechaFin()); date = date.plusDays(1)) {
            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Determinar horas de inicio y fin según el día de la semana
            int startHour;
            int endHour;

            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                startHour = 8;
                endHour = 13;
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                // Domingo: sin citas
                continue;
            } else {
                // Lunes a viernes
                startHour = 8;
                endHour = 18;
            }

            // Generar intervalos de 30 minutos para este día
            for (int hour = startHour; hour < endHour; hour++) {
                for (int minute = 0; minute < 60; minute += 30) {
                    LocalDateTime slotTime = date.atTime(hour, minute);

                    // Verificar si este intervalo ya está reservado
                    LocalDateTime slotStart = slotTime;
                    LocalDateTime slotEnd = slotTime.plusMinutes(30);

                    List<Appointment> existingAppointments =
                            appointmentRepositoryPort.findByAppointmentDateBetween(slotStart, slotEnd);

                    boolean isBooked = false;
                    for (Appointment appt : existingAppointments) {
                        if (appt.getDoctorId().equals(query.doctorId())
                                && !AppointmentStatus.CANCELADA.equals(appt.getStatus())) {
                            isBooked = true;
                            break;
                        }
                    }

                    if (!isBooked) {
                        availableSlots.add(new AvailableSlot(
                                slotTime,
                                date,
                                String.format("%02d:%02d", hour, minute),
                                dayOfWeek.toString()
                        ));
                    }
                }
            }
        }

        return availableSlots;
    }
}
