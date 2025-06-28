package com.esprit.formation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    @NotNull
    private Long formationId;
    private LocalDate dateDebut;
    private Long sessionEventId;
    @NotNull
    private String couponId;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
}
