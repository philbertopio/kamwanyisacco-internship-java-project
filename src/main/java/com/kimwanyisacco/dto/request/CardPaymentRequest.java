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
public class CardPaymentRequest {

    @NotNull(message = "Payment purpose is required")
    private PaymentPurpose purpose;

    @NotNull(message = "Target account/loan is required")
    private Long targetId;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required")
    private String cardHolderName;

    @NotNull(message = "Expiry month is required")
    @javax.validation.constraints.Min(value = 1, message = "Invalid month")
    @javax.validation.constraints.Max(value = 12, message = "Invalid month")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    private Integer expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    @NotBlank(message = "Please select a card scheme")
    @Pattern(regexp = "VISA|MASTERCARD", message = "Card scheme must be VISA or MASTERCARD")
    private String cardScheme;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "500.00", message = "Minimum payment amount is UGX 500")
    private BigDecimal amount;
}
