package com.esprit.formation.controller;


import com.esprit.formation.dto.ParticipationStatsDTO;
import com.esprit.formation.dto.StatsResponse;
import com.esprit.formation.dto.UserExpenseDTO;
import com.esprit.formation.iservices.IStatService;
import com.esprit.formation.utils.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/stats")
@Tag(name = "Stats-End-Point", description = "Endpoints for platform statistics")
public class StatController {

    private final IStatService statService;

    @Operation(summary = "Get platform statistics",
            description = "Retrieve overall platform statistics including total transactions amount, number of formations, participants and formateurs")
    @GetMapping
    public ResponseEntity<?> getPlatformStats() {
        try {
            StatsResponse stats = statService.getPlatformStats();
            return ResponseWrapper.success(stats);
        } catch (Exception e) {
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching platform statistics: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Récupérer les statistiques de participation",
            description = """
        Retourne deux types de statistiques :
        1. Par formation: Nombre de participants par formation
        2. Par jour: Nombre de transactions confirmées par date
        
        Seules les transactions avec statut CONFIRMED sont comptabilisées.
        """
    )
    @GetMapping("/participations")
    public ResponseEntity<?> getParticipationStats() {
        try {
            ParticipationStatsDTO stats = statService.getParticipationStats();
            return ResponseWrapper.success(stats);
        } catch (Exception e) {
            return ResponseWrapper.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching participation stats: " + e.getMessage()
            );
        }
    }

    @Operation(summary = "Get user expense statistics",
            description = "Retrieve statistics about each user's formation purchases including total count and amount spent")
    @GetMapping("/user-expenses")
    public ResponseEntity<?> getUserExpenseStats() {
        try {
            List<UserExpenseDTO> stats = statService.getUserExpenseStats();
            return ResponseWrapper.success(stats);
        } catch (Exception e) {
            return ResponseWrapper.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching user expense stats: " + e.getMessage()
            );
        }
    }
}
