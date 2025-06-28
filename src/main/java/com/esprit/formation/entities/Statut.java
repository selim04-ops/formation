package com.esprit.formation.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Statut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic
    @JsonIgnore
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenuStatut;

    @Builder.Default
    private Boolean activeStatus = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Reference to the User entity

    @Builder.Default
    @OneToMany(mappedBy = "statut", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commentaire> commentaires= new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime date;


    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "statut_images", joinColumns = @JoinColumn(name = "statut_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> images=new ArrayList<>(); // List of image URLs
}
