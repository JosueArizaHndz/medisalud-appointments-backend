package com.medisalud.appointments.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Debe asociar al paciente")
    @Column(columnDefinition = "BINARY(16)")
    private UUID patientId;

    @NotBlank(message = "El tipo de penalización es obligatorio")
    private String penaltyType;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
