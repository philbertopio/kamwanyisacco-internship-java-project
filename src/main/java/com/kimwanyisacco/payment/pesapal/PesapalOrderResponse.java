package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the JSON body returned by POST /api/Transactions/SubmitOrderRequest.
 *
 * <pre>
 * {
 *   "order_tracking_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
 *   "merchant_reference": "SACCO-42-A1B2C3D4",
 *   "redirect_url": "https://pay.pesapal.com/iframe/PesapalIframe3?OrderTrackingId=...",
 *   "error": null,
 *   "status": "200"
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PesapalOrderResponse {

    /** PesaPal's tracking id for this order — used to poll GetTransactionStatus. */
    @JsonProperty("order_tracking_id")
    private String orderTrackingId;

    /** Echo of the merchant reference we sent. */
    @JsonProperty("merchant_reference")
    private String merchantReference;

    /** URL the member must visit to complete the payment. */
    @JsonProperty("redirect_url")
    private String redirectUrl;

    @JsonProperty("error")
    private PesapalError error;

    @JsonProperty("status")
    private String status;
}
