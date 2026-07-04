package com.medisalud.appointments.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    private String email;

    @NotBlank(message = "La especialidad es obligatoria")
    private String specialty;

    @Pattern(regexp = "^\\d{7,}$", message = "El teléfono debe tener mínimo 7 dígitos")
    private String phone;

    @Column(unique = true)
    private String licenseNumber;

    @Positive(message = "El número de pacientes máximos debe ser positivo")
    private Integer maxPatients;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
