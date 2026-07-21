package com.kimwanyisacco.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentRequest {

    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Repayment amount must be at least 1.00")
    private BigDecimal amount;
}
