package com.esprit.formation.dto;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionUser {
    private Long userId;
    private String email;
    private String nomEtPrenom;
    private Long phoneNumber;
}
