package com.kimwanyisacco.dto.response;

import com.kimwanyisacco.model.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {

    private Long id;
    private Long memberId;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalRepayable;
    private BigDecimal amountRepaid;
    private BigDecimal outstandingBalance;
    private LoanStatus status;
    private LocalDate applicationDate;
    private LocalDate dueDate;
}

