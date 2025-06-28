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
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Reference to the User entity

    @Builder.Default
    private Boolean activeCommentaire = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id", nullable = false)
    private Statut statut; // Reference to the Statut entity

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenuCommentaire;

    @Column(nullable = false)
    private LocalDateTime date;
}
