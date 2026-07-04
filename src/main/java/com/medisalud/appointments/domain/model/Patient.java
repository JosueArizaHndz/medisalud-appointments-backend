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
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 7, max = 20, message = "La cédula debe tener entre 7 y 20 caracteres")
    @Column(unique = true)
    private String identityDocument;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{7,}$", message = "El teléfono debe tener mínimo 7 dígitos")
    private String phone;

    private LocalDate birthDate;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @PositiveOrZero(message = "El número de historias debe ser positivo o cero")
    private Integer medicalRecordNumber;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
