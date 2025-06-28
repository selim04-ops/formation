package com.esprit.formation.dto;

import com.esprit.formation.entities.Formation;
import com.esprit.formation.entities.SessionEvent;
import com.esprit.formation.entities.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@Data
@Getter
@Setter
public class SessionEventDTO {
    private Long id;
    private String titre;
    private String description;
    private String type;
    private Set<Long> formationIds;
    private Set<Long> formateurIds;
    private Set<Long> participantIds;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
    private String statut;
    private Integer capaciteMax;
    private List<String> images;


    public static SessionEventDTO fromEntity(SessionEvent sessionEvent) {
        return SessionEventDTO.builder()
                .id(sessionEvent.getId())
                .titre(sessionEvent.getTitre())
                .description(sessionEvent.getDescription())
                .type(sessionEvent.getType())
                .formationIds(sessionEvent.getFormations().stream().map(Formation::getId).collect(Collectors.toSet()))
                .formateurIds(sessionEvent.getFormateurs().stream().map(User::getId).collect(Collectors.toSet()))
                .participantIds(sessionEvent.getParticipants().stream().map(User::getId).collect(Collectors.toSet()))
                .dateDebut(sessionEvent.getDateDebut())
                .dateFin(sessionEvent.getDateFin())
                .lieu(sessionEvent.getLieu())
                .statut(sessionEvent.getStatut())
                .capaciteMax(sessionEvent.getCapaciteMax())
                .images(sessionEvent.getImages() != null ? sessionEvent.getImages() : Collections.emptyList())
                .build();
    }
}