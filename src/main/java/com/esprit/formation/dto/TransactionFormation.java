package com.esprit.formation.dto;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFormation {
    private Long formationId;
    private String titre;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private LocalDate dateDebut;
    private String codeCoupon;
}
