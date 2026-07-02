package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.AppointmentResponse;
import com.medisalud.appointments.application.dto.AvailableSlot;
import com.medisalud.appointments.domain.model.Appointment;
import com.medisalud.appointments.domain.port.in.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentServiceInterface {
    List<Appointment> getAllAppointments();
    Appointment getAppointmentById(UUID id);
    List<Appointment> getAppointmentsByDoctorId(UUID doctorId);
    List<Appointment> getAppointmentsByPatientId(UUID patientId);
    List<Appointment> getAppointmentsByDate(LocalDate date);
    AppointmentResponse createAppointment(CreateAppointmentCommand command);
    AppointmentResponse cancelAppointment(CancelAppointmentCommand command);
    AppointmentResponse rescheduleAppointment(RescheduleAppointmentCommand command);
    AppointmentResponse mapToResponse(Appointment appointment);
    List<AvailableSlot> getAvailableSlots(AvailableSlotsQuery query);
    List<AppointmentResponse> listAppointments(ListAppointmentsQuery query);
}
