package com.esprit.formation.dto;


import com.esprit.formation.entities.Categorie;
import com.esprit.formation.entities.NiveauFormation;
import com.esprit.formation.entities.Type;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormationRequest {
    @NotBlank(message = "Le titre ne peut pas être vide")
    private String titre;
    private String description;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateDebut;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateFin;
    private double prix;
    private NiveauFormation niveau;
    private List<String> imageUrls;
    private List<Long> participantsIds;
    private List<Long> formateursIds;
    private Type type;
    private Categorie categorie;
}

