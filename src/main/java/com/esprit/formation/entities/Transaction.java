package com.esprit.formation.entities;

import com.esprit.formation.dto.TransactionFormation;
import com.esprit.formation.dto.TransactionSessionEvent;
import com.esprit.formation.dto.TransactionUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private TransactionUser user;



    @ElementCollection
    @CollectionTable(name = "transaction_formations", joinColumns = @JoinColumn(name = "transaction_id"))
    private List<TransactionFormation> formations;

    @ElementCollection
    @CollectionTable(name = "transaction_session-event", joinColumns = @JoinColumn(name = "transaction_id"))
    private List<TransactionSessionEvent> sessionEvents;



    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentMethode paymentMethod=PaymentMethode.CASH;


    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDate expiresAt;

    private String stripePaymentId;
    private String receiptUrl;
    private String adminNotes;

    @PositiveOrZero
    private BigDecimal totalPrice;

    private String currency;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @NotNull
    private TransactionStatus transactionStatus = TransactionStatus.PENDING;

    // Add helper methods
    public boolean isPaid() {
        return transactionStatus == TransactionStatus.COMPLETED;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDate.now());
    }

    // Consider adding formation titles concatenated for easy access
    @Transient
    public String getFormationTitles() {
        return formations.stream()
                .map(TransactionFormation::getTitre)
                .collect(Collectors.joining(", "));
    }
}