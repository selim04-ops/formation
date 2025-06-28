package com.esprit.formation.dto;


import com.esprit.formation.entities.Categorie;
import com.esprit.formation.entities.EtatFormation;
import com.esprit.formation.entities.NiveauFormation;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Getter
@Setter
public class FormationResponse {
    private Long id;
    private String titre;
    private String description;
    private Boolean active;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateFin;

    private double prix;
    private EtatFormation etatFormation;
    private NiveauFormation niveau;
    private List<String> images;
    private List<Long> participantIds;
    private List<Long> formateurIds;
    private String type;
    private Categorie categorie;
    private CouponResponse coupon;

}


