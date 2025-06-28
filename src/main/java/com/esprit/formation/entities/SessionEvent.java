package com.esprit.formation.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.*;
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SessionEvent {


        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String titre;
        private String description;
        private String type;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "session_formations",
            joinColumns = @JoinColumn(name = "session_event_id"),
            inverseJoinColumns = @JoinColumn(name = "formation_id")
    )
    private Set<Formation> formations = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "session_event_formateurs",
            joinColumns = @JoinColumn(name = "session_event_id"),
            inverseJoinColumns = @JoinColumn(name = "formateur_id")
    )
    private Set<User> formateurs = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "session_event_participants",
            joinColumns = @JoinColumn(name = "session_event_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private Set<User> participants = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "session_event_images", joinColumns = @JoinColumn(name = "session_event_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> images= new ArrayList<>();

        private LocalDate dateDebut;

        private LocalDate dateFin;

        private String lieu;

        private String statut;

        private Integer capaciteMax;

    @OneToMany(mappedBy = "sessionEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FeedbackSession> feedbacks = new ArrayList<>();


    public void addFeedback(FeedbackSession feedback) {
        feedbacks.add(feedback);
        feedback.setSessionEvent(this);
    }



}
