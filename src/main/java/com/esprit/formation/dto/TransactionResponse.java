package com.esprit.formation.dto;

import com.esprit.formation.entities.PaymentMethode;
import com.esprit.formation.entities.Transaction;
import com.esprit.formation.entities.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class TransactionResponse {
        private Long id;
        private String userAvatar;
        private String userEmail;
        private Long userPhone;
        private String nomEtPrenom;
        private PaymentMethode paymentMethod;
        private BigDecimal totalPrice;
        private TransactionStatus transactionStatus;
        private LocalDateTime createdAt;
        private List<TransactionFormation> formations;

        public TransactionResponse(Transaction transaction) {
            this.id = transaction.getId();
            this.nomEtPrenom = transaction.getUser().getNomEtPrenom();
            this.paymentMethod = transaction.getPaymentMethod();
            this.totalPrice = transaction.getTotalPrice();
            this.transactionStatus = transaction.getTransactionStatus();
            this.createdAt = transaction.getCreatedAt();
            this.formations = transaction.getFormations();
        }
    }

