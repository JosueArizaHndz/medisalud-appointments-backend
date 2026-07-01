package com.medisalud.appointments.application.service;

import com.medisalud.appointments.application.dto.PatientResponse;
import com.medisalud.appointments.domain.model.Patient;
import com.medisalud.appointments.domain.port.in.*;
import java.util.List;
import java.util.UUID;

public interface PatientServiceInterface {
    List<Patient> getAllPatients();
    Patient getPatientById(UUID id);
    Patient getPatientByIdentityDocument(String identityDocument);
    PatientResponse createPatient(CreatePatientCommand command);
    PatientResponse updatePatient(UUID id, UpdatePatientCommand command);
    void deletePatient(DeletePatientCommand command);
}
