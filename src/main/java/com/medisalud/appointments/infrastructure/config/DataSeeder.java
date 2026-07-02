package com.medisalud.appointments.infrastructure.config;

import com.medisalud.appointments.domain.model.Doctor;
import com.medisalud.appointments.domain.port.out.DoctorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DoctorRepositoryPort doctorRepositoryPort;

    @Override
    @Transactional
    public void run(String... args) {
        seedDoctors();
    }

    @Transactional
    private void seedDoctors() {
        try {
            if (doctorRepositoryPort.findAll().isEmpty()) {
                Doctor doctor1 = Doctor.builder()
                        .name("Dra. María González")
                        .email("maria.gonzalez@medisalud.com")
                        .specialty("Cardiología")
                        .licenseNumber("555-1001")
                        .maxPatients(30)
                        .active(true)
                        .build();

                Doctor doctor2 = Doctor.builder()
                        .name("Dr. Carlos Ruiz")
                        .email("carlos.ruiz@medisalud.com")
                        .specialty("Pediatría")
                        .licenseNumber("555-1002")
                        .maxPatients(40)
                        .active(true)
                        .build();

                Doctor doctor3 = Doctor.builder()
                        .name("Dra. Ana López")
                        .email("ana.lopez@medisalud.com")
                        .specialty("Dermatología")
                        .licenseNumber("555-1003")
                        .maxPatients(25)
                        .active(true)
                        .build();

                doctorRepositoryPort.save(doctor1);
                doctorRepositoryPort.save(doctor2);
                doctorRepositoryPort.save(doctor3);

                System.out.println("✅ Seed inicial cargado: 3 doctores creados");
            } else {
                System.out.println("ℹ️ Los datos iniciales ya existen, no se realizó seed");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar seed: " + e.getMessage());
        }
    }
}
