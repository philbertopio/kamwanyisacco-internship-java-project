package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic PesaPal error object embedded inside most API responses.
 *
 * <pre>
 * {
 *   "error_type": "invalid_client",
 *   "code": "...",
 *   "message": "Human-readable description"
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PesapalError {

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;
}
