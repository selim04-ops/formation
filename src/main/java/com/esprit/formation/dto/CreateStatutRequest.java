package com.esprit.formation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Builder
@Getter
@Setter
public class CreateStatutRequest {
    @NotBlank(message = "Le contenu ne peut pas être vide")
    private String content;
    private List<String> imageUrls;
}
