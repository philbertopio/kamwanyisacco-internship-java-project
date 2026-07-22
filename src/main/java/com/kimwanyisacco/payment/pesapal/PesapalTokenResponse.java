package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the JSON body returned by POST /api/Auth/RequestToken.
 *
 * <pre>
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "expiryDate": "2024-01-01T12:34:56.000Z",
 *   "error": null,
 *   "status": "200",
 *   "message": "Request processed successfully"
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PesapalTokenResponse {

    @JsonProperty("token")
    private String token;

    /** UTC timestamp string, e.g. "2024-01-01T12:34:56.000Z". */
    @JsonProperty("expiryDate")
    private String expiryDate;

    @JsonProperty("error")
    private PesapalError error;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}
