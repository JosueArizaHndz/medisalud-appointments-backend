package com.medisalud.appointments.domain.port.in;

import com.medisalud.appointments.domain.model.Doctor;
import java.util.List;
import java.util.UUID;

public interface DoctorQueryPort {
    List<Doctor> getAllDoctors();
    Doctor getDoctorById(UUID id);
    List<Doctor> getActiveDoctors();
    Doctor getDoctorByEmail(String email);
}
