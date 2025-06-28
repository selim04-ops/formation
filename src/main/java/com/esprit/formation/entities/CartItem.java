package com.esprit.formation.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cart_items")
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;


    private Long formationId;
    private Long sessionEventId;
    private LocalDate dateDebut;

    private String appliedCouponId;

    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;

    }
