package com.esprit.formation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class CreateCommentaireRequest {
    @NotBlank(message = "Le contenu ne peut pas être vide")
    private String content;

    @NotNull(message = "L'ID du statut est requis")
    private Long statutId;
}

