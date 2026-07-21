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
public class LoanApplicationRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is UGX 1,000")
    private BigDecimal principalAmount;
}
