package com.esprit.formation.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "revoked-Token")
public class RevokedToken {
    @Id
    private String token;

    @Builder.Default
    private LocalDateTime revokedAt = LocalDateTime.now();
}
