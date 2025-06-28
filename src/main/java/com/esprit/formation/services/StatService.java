package com.esprit.formation.services;

import com.esprit.formation.dto.ParticipationStatsDTO;
import com.esprit.formation.dto.StatsResponse;
import com.esprit.formation.dto.UserExpenseDTO;
import com.esprit.formation.entities.Role;
import com.esprit.formation.entities.Transaction;
import com.esprit.formation.entities.TransactionStatus;
import com.esprit.formation.iservices.IStatService;
import com.esprit.formation.repository.FormationRepository;
import com.esprit.formation.repository.TransactionRepository;
import com.esprit.formation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService implements IStatService {

    private final TransactionRepository transactionRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;

    @Override
    public StatsResponse getPlatformStats() {
        // Calculate total amount of CONFIRMED transactions only
        BigDecimal totalAmount = transactionRepository
                .findByTransactionStatus(TransactionStatus.CONFIRMED)
                .stream()
                .map(Transaction::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalFormations = formationRepository.count();
        long totalParticipants = userRepository.countByRole(Role.PARTICIPANT);
        long totalFormateurs = userRepository.countByRole(Role.FORMATEUR);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        return StatsResponse.builder()
                .totalConfirmedTransactionsAmount(totalAmount)
                .totalFormations(totalFormations)
                .totalParticipants(totalParticipants)
                .totalFormateurs(totalFormateurs)
                .totalAdmins(totalAdmins)
                .build();
    }

    public ParticipationStatsDTO getParticipationStats() {
        return ParticipationStatsDTO.builder()
                .formations(mapFormationStats())
                .days(mapDailyStats())
                .build();
    }

    private List<ParticipationStatsDTO.FormationStat> mapFormationStats() {
        return transactionRepository.countParticipantsByFormation()
                .stream()
                .map(p -> ParticipationStatsDTO.FormationStat.builder()
                        .formationId(p.getFormationId())
                        .formationTitle(p.getTitle())
                        .participantCount(p.getCount().intValue())
                        .build())
                .toList();
    }

    private List<ParticipationStatsDTO.DailyStat> mapDailyStats() {
        return transactionRepository.countTransactionsByDate()
                .stream()
                .map(d -> ParticipationStatsDTO.DailyStat.builder()
                        .date(d.getDate().toString())
                        .transactionCount(d.getCount().intValue())
                        .build())
                .toList();
    }


    @Override
    public List<UserExpenseDTO> getUserExpenseStats() {
        return transactionRepository.findByTransactionStatus(TransactionStatus.CONFIRMED)
                .stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getUser().getUserId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                transactions -> {
                                    Transaction first = transactions.get(0);
                                    return UserExpenseDTO.builder()
                                            .userId(first.getUser().getUserId())
                                            .userFullName(first.getUser().getNomEtPrenom())
                                            .userEmail(first.getUser().getEmail())
                                            .formationCount(transactions.size())
                                            .totalExpense(transactions.stream()
                                                    .map(Transaction::getTotalPrice)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                                            .build();
                                }
                        )
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(UserExpenseDTO::getTotalExpense).reversed())
                .collect(Collectors.toList());
    }
}