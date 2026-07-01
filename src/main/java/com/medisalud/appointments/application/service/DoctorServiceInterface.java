package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.DoctorResponse;
import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.port.in.*;
import java.util.List;
import java.util.UUID;

public interface DoctorServiceInterface {
    List<Doctor> getAllDoctors();
    Doctor getDoctorById(UUID id);
    List<Doctor> getActiveDoctors();
    DoctorResponse createDoctor(CreateDoctorCommand command);
    DoctorResponse updateDoctor(UUID id, UpdateDoctorCommand command);
    void deleteDoctor(DeleteDoctorCommand command);
}
