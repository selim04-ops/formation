package com.esprit.formation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StatsResponse {
    private BigDecimal totalConfirmedTransactionsAmount;
    private long totalFormations;
    private long totalParticipants;
    private long totalFormateurs;
    private long totalAdmins;
}