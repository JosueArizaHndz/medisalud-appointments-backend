package com.medisalud.appointments.domain.port.out;

import com.medisalud.appointments.domain.model.Penalty;
import java.util.List;
import java.util.UUID;

public interface PenaltyRepositoryPort {
    Penalty save(Penalty penalty);
    List<Penalty> findByPatientId(UUID patientId);
    void deleteById(UUID id);
}
