package com.esprit.formation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {
    @NotBlank(message = "Le code ne peut pas être vide")
    private String code;

    @Positive(message = "La réduction doit être positive")
    @Max(value = 100, message = "La réduction ne peut pas dépasser 100%")
    private Double discount;

    @Positive(message = "Le nombre maximum d'utilisations doit être positif")
    private Integer maxUsage;

   // @NotNull(message = "L'ID de la formation est requis")
   private Set<Long> formationIds;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Future
    private LocalDate expireAt;
}