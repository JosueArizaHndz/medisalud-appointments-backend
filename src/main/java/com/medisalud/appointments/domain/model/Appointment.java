package com.medisalud.appointments.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Debe seleccionar un paciente")
    @Column(columnDefinition = "BINARY(16)")
    private UUID patientId;

    @NotNull(message = "Debe seleccionar un doctor")
    @Column(columnDefinition = "BINARY(16)")
    private UUID doctorId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "El estado es obligatorio")
    private String status;

    private String notes;

    private LocalDateTime cancellationDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
