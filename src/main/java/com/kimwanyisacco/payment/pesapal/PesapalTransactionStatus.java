package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Maps the JSON body returned by GET /api/Transactions/GetTransactionStatus.
 *
 * <pre>
 * {
 *   "payment_method":              "MTN Uganda",
 *   "amount":                      10000.00,
 *   "created_date":                "2024-01-01T09:00:00.000Z",
 *   "confirmation_code":           "ABC123XYZ",
 *   "payment_status_description":  "Completed",
 *   "description":                 "",
 *   "message":                     "Request processed successfully",
 *   "payment_account":             "256771234567",
 *   "call_back_url":               "https://...",
 *   "status_code":                 1,
 *   "merchant_reference":          "SACCO-42-A1B2C3D4",
 *   "payment_status_code":         "1",
 *   "currency":                    "UGX",
 *   "error":                       null,
 *   "status":                      "200"
 * }
 * </pre>
 *
 * <p><strong>status_code meanings (from PesaPal docs):</strong>
 * <ul>
 *   <li>0 = INVALID (not found / not processed yet)</li>
 *   <li>1 = COMPLETED</li>
 *   <li>2 = FAILED</li>
 *   <li>3 = REVERSED</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PesapalTransactionStatus {

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("created_date")
    private String createdDate;

    /** Provider-side confirmation / receipt code. */
    @JsonProperty("confirmation_code")
    private String confirmationCode;

    /** Human-readable status string from PesaPal (e.g. "Completed", "Failed"). */
    @JsonProperty("payment_status_description")
    private String paymentStatusDescription;

    @JsonProperty("description")
    private String description;

    @JsonProperty("message")
    private String message;

    /** The payer's phone number or card account. */
    @JsonProperty("payment_account")
    private String paymentAccount;

    /**
     * Numeric status code:
     * 0=INVALID, 1=COMPLETED, 2=FAILED, 3=REVERSED.
     */
    @JsonProperty("status_code")
    private Integer statusCode;

    @JsonProperty("merchant_reference")
    private String merchantReference;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("error")
    private PesapalError error;

    /** HTTP-level status from PesaPal (usually "200"). */
    @JsonProperty("status")
    private String status;
}
