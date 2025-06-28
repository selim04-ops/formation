package com.esprit.formation.config;

import com.esprit.formation.entities.EtatFormation;
import com.esprit.formation.entities.Formation;
import com.esprit.formation.entities.Transaction;
import com.esprit.formation.entities.TransactionStatus;
import com.esprit.formation.repository.FormationRepository;
import com.esprit.formation.repository.PasswordResetTokenRepository;
import com.esprit.formation.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor // For dependency injection
public class CronConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CronConfig.class);

    private final PasswordResetTokenRepository tokenRepository;

    // Define your cron expressions as constants
    public static final String DAILY_AT_3AM = "0 0 3 * * ?";
    public static final String DAILY_AT_MIDNIGHT_1MIN = "0 1 0 * * ?";

    private final TransactionRepository transactionRepository;
    private final FormationRepository formationRepository;

    @Scheduled(cron = DAILY_AT_3AM )
    //@Scheduled(fixedRate = 500000)

    @Transactional
    public void cleanExpiredPasswordResetTokens() {
        int deactivatedCount = tokenRepository.deactivateExpiredTokens(new Date());

        // Optional: Log or monitor the cleanup

         LOGGER.info("the PasswordResetToken is cleaned-up : Deactivated {} expired tokens", deactivatedCount);
    }


    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireOldTransactions() {
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        int expiredCount = 0;

        for (Transaction transaction : pendingTransactions) {
            boolean shouldExpire = transaction.getFormations().stream()
                    .anyMatch(formation -> {
                        LocalDate formationDate = formationRepository.findById(formation.getFormationId())
                                .orElseThrow().getDateDebut();
                        return formationDate.isBefore(LocalDate.now());
                    });

            if (shouldExpire) {
                transactionRepository.updateTransactionStatus(transaction.getId(), TransactionStatus.EXPIRED);
                expiredCount++;
            }
        }

        LOGGER.info("Marked {} pending transactions as EXPIRED", expiredCount);
    }


    @Scheduled(cron = DAILY_AT_MIDNIGHT_1MIN)
    //@Scheduled(fixedRate = 500000)

    @Transactional
    public void updateFormationStatuses() {
        LocalDate today = LocalDate.now();
        LOGGER.info("Starting formation status update at {}", LocalDateTime.now());

        // Get all formations that need status updates
        List<Formation> formations = formationRepository.findAll();
        int updatedCount = 0;

        for (Formation formation : formations) {
            EtatFormation newStatus = determineFormationStatus(formation, today);

            if (newStatus != formation.getEtatFormation()) {
                formation.setEtatFormation(newStatus);
                formationRepository.save(formation);
                updatedCount++;
                LOGGER.debug("Updated formation {} (ID: {}) to status: {}",
                        formation.getTitre(), formation.getId(), newStatus);
            }
        }

        LOGGER.info("Completed formation status update. {} formations updated.", updatedCount);
    }

    private EtatFormation determineFormationStatus(Formation formation, LocalDate today) {
        if (today.isBefore(formation.getDateDebut())) {
            return EtatFormation.A_VENIR;
        } else if (today.isAfter(formation.getDateFin())) {
            return EtatFormation.TERMINEE;
        } else {
            return EtatFormation.EN_COURS;
        }
    }

}
