package com.esprit.formation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ParticipationStatsDTO {
    // Pour les stats par formation
    @Builder.Default
    private List<FormationStat> formations = List.of();

    // Pour les stats par jour
    @Builder.Default
    private List<DailyStat> days = List.of();

    @Getter
    @Builder
    public static class FormationStat {
        private Long formationId;
        private String formationTitle;
        private Integer participantCount;
    }

    @Getter
    @Builder
    public static class DailyStat {
        private String date; // Format "yyyy-MM-dd"
        private Integer transactionCount;
    }
}