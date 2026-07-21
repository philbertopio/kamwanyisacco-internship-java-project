package com.kimwanyisacco.dto.request;

import com.kimwanyisacco.model.enums.PaymentPurpose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyPaymentRequest {

    @NotNull(message = "Payment purpose is required")
    private PaymentPurpose purpose;

    // Savings account id (for SAVINGS_DEPOSIT) or loan id (for LOAN_REPAYMENT)
    @NotNull(message = "Target account/loan is required")
    private Long targetId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Enter a valid phone number")
    private String phoneNumber;

    @NotBlank(message = "Please select a mobile money provider")
    @Pattern(regexp = "MTN|AIRTEL", message = "Provider must be MTN or AIRTEL")
    private String mobileProvider;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "500.00", message = "Minimum payment amount is UGX 500")
    private BigDecimal amount;
}
