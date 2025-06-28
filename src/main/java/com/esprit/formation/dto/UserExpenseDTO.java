package com.esprit.formation.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserExpenseDTO {
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Integer formationCount;
    private BigDecimal totalExpense;
}
