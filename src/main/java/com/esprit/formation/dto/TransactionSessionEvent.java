package com.esprit.formation.dto;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSessionEvent {


    private Long sessionEventId;
    private String titre;
    private String type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String lieu;
}
