package com.medisalud.appointments.domain.port.in;

import com.medisalud.appointments.domain.model.Patient;
import java.util.List;
import java.util.UUID;

public interface PatientQueryPort {
    List<Patient> getAllPatients();
    Patient getPatientById(UUID id);
    Patient getPatientByIdentityDocument(String identityDocument);
    List<Patient> getActivePatients();
}
