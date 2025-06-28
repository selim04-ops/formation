package com.esprit.formation.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long formationId;
    private String formationName;
    private Long sessionEventId;
    private String sessionEventTitre;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;

    // Constructor must match the SELECT statement in the query
    public CartItemResponse(
            Long id,
            Long formationId,
            String formationName,
            Long sessionEventId,
            String sessionEventTitre,
            BigDecimal originalPrice,
            BigDecimal discountedPrice
    ) {
        this.id = id;
        this.formationId = formationId;
        this.formationName = formationName;
        this.sessionEventId = sessionEventId;
        this.sessionEventTitre = sessionEventTitre;
        this.originalPrice = originalPrice;
        this.finalPrice = discountedPrice;
    }
}