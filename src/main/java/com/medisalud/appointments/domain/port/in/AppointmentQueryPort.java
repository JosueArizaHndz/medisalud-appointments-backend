package com.medisalud.appointments.domain.port.in;

import com.medisalud.appointments.domain.model.Appointment;
import java.util.List;
import java.util.UUID;

public interface AppointmentQueryPort {
    List<Appointment> getAllAppointments();
    Appointment getAppointmentById(UUID id);
    List<Appointment> getAppointmentsByDoctorId(UUID doctorId);
    List<Appointment> getAppointmentsByPatientId(UUID patientId);
    List<Appointment> getAppointmentsByDate(java.time.LocalDate date);
}
