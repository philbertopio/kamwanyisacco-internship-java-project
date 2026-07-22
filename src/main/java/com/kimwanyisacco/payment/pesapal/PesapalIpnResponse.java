package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the JSON body returned by POST /api/URLSetup/RegisterIPN.
 *
 * <pre>
 * {
 *   "url": "https://yourdomain.com/api/v1/payments/ipn",
 *   "created_date": "2024-01-01T09:00:00.000Z",
 *   "ipn_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
 *   "error": null,
 *   "status": "200"
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PesapalIpnResponse {

    @JsonProperty("url")
    private String url;

    @JsonProperty("created_date")
    private String createdDate;

    /** The IPN id — required in every SubmitOrderRequest as "notification_id". */
    @JsonProperty("ipn_id")
    private String ipnId;

    @JsonProperty("error")
    private PesapalError error;

    @JsonProperty("status")
    private String status;
}
