package com.kimwanyisacco.payment.pesapal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kimwanyisacco.config.ConfigLoader;
import com.kimwanyisacco.exception.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * PesaPal v3 HTTP client — mirrors the Python {@code payment_service.py} logic.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@link #getBearerToken()} — POST /api/Auth/RequestToken, caches token for ~5 min</li>
 *   <li>{@link #getIpnId()} — POST /api/URLSetup/RegisterIPN, caches ipn_id for lifetime</li>
 *   <li>{@link #submitOrder} — POST /api/Transactions/SubmitOrderRequest → redirect_url</li>
 *   <li>{@link #getTransactionStatus} — GET /api/Transactions/GetTransactionStatus</li>
 * </ol>
 *
 * <p>All credentials are read from {@link ConfigLoader} which pulls from {@code .env}
 * (local dev) or real system environment variables (production/CI).
 *
 * <p>Uses the built-in {@code java.net.http.HttpClient} (JDK 11+) — no extra dependency.
 */
@Service
public class PesapalGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PesapalGatewayService.class);

    // ── Config (read once from ConfigLoader) ─────────────────────────────────
    private final String baseUrl;
    private final String consumerKey;
    private final String consumerSecret;
    private final String callbackUrl;
    private final String ipnUrl;
    private final String currency;
    private final String businessName;

    // ── Shared HTTP client and JSON mapper ───────────────────────────────────
    private final HttpClient http;
    private final ObjectMapper json;

    // ── In-memory token cache ────────────────────────────────────────────────
    private volatile String cachedToken      = null;
    private volatile Instant tokenExpiresAt  = Instant.EPOCH;

    // ── In-memory IPN id cache (PesaPal de-duplicates on same URL) ───────────
    private volatile String cachedIpnId = null;

    public PesapalGatewayService() {
        this.baseUrl       = ConfigLoader.get("pesapal.base.url");
        this.consumerKey   = ConfigLoader.get("pesapal.consumer.key");
        this.consumerSecret= ConfigLoader.get("pesapal.consumer.secret");
        this.callbackUrl   = ConfigLoader.get("pesapal.callback.url");
        this.ipnUrl        = ConfigLoader.get("pesapal.ipn.url");
        this.currency      = ConfigLoader.get("payment.currency");
        this.businessName  = ConfigLoader.get("payment.business.name");

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.json = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a valid PesaPal bearer token.
     * Fetches a new one when missing or within 30 s of expiry (same strategy as Python impl).
     */
    public synchronized String getBearerToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedToken;
        }

        log.info("Fetching fresh PesaPal bearer token");
        log.debug("PesaPal base URL: {}", baseUrl);

        String body = toJson(Map.of(
                "consumer_key",    consumerKey,
                "consumer_secret", consumerSecret
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/Auth/RequestToken"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = executeRequest(request, "Auth/RequestToken");

        if (response.statusCode() != 200) {
            log.error("PesaPal auth failed — HTTP {}: {}", response.statusCode(), response.body());
            throw new PaymentProcessingException(
                    "Payment gateway authentication failed. Check PesaPal credentials.");
        }

        PesapalTokenResponse tokenResp = parseJson(response.body(), PesapalTokenResponse.class);

        if (tokenResp.getError() != null && tokenResp.getError().getMessage() != null) {
            throw new PaymentProcessingException(
                    "PesaPal auth error: " + tokenResp.getError().getMessage());
        }

        cachedToken = tokenResp.getToken();

        // Parse expiry date (ISO-8601 UTC like "2024-01-01T12:34:56.000Z")
        try {
            tokenExpiresAt = ZonedDateTime.parse(tokenResp.getExpiryDate()).toInstant();
        } catch (Exception e) {
            // Fallback: treat token as valid for 4 minutes
            tokenExpiresAt = now.plusSeconds(240);
            log.warn("Could not parse PesaPal token expiry '{}', defaulting to 4 min",
                     tokenResp.getExpiryDate());
        }

        log.info("PesaPal token acquired, expires at {}", tokenExpiresAt);
        return cachedToken;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPN REGISTRATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the IPN id for our registered callback URL.
     *
     * <p>PesaPal de-duplicates registrations: re-registering the same URL
     * returns the existing {@code ipn_id}, so this is safe to call on every
     * app startup or first payment.
     */
    public synchronized String getIpnId() {
        if (cachedIpnId != null) {
            return cachedIpnId;
        }

        log.info("Registering IPN URL with PesaPal: {}", ipnUrl);

        String body = toJson(Map.of(
                "url",                   ipnUrl,
                "ipn_notification_type", "GET"   // PesaPal sends a GET to our IPN endpoint
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/URLSetup/RegisterIPN"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + getBearerToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = executeRequest(request, "URLSetup/RegisterIPN");

        if (response.statusCode() != 200) {
            log.error("IPN registration failed — HTTP {}: {}", response.statusCode(), response.body());
            throw new PaymentProcessingException("Failed to register IPN URL with PesaPal.");
        }

        PesapalIpnResponse ipnResp = parseJson(response.body(), PesapalIpnResponse.class);

        if (ipnResp.getIpnId() == null || ipnResp.getIpnId().isEmpty()) {
            throw new PaymentProcessingException(
                    "PesaPal IPN registration returned no ipn_id: " + response.body());
        }

        cachedIpnId = ipnResp.getIpnId();
        log.info("PesaPal IPN registered, ipn_id={}", cachedIpnId);
        return cachedIpnId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT ORDER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submits an order to PesaPal and returns the hosted checkout URL.
     *
     * <p>Both Card and Mobile Money payments use this same endpoint.
     * PesaPal presents the payment method picker on their hosted page.
     *
     * @param merchantReference Unique reference for this attempt (max 50 chars)
     * @param amount            Payment amount
     * @param description       Short narration shown on PesaPal page (max 100 chars)
     * @param firstName         Customer first name
     * @param lastName          Customer last name
     * @param email             Customer email (required by PesaPal)
     * @param phone             Customer phone (optional but improves MoMo pre-fill)
     * @param branch            Branch / shop name
     * @return PesapalOrderResponse containing redirect_url and order_tracking_id
     */
    public PesapalOrderResponse submitOrder(
            String merchantReference,
            BigDecimal amount,
            String description,
            String firstName,
            String lastName,
            String email,
            String phone,
            String branch) {

        String ipnId = getIpnId();

        // Truncate description to PesaPal's 100-char limit
        String safeDesc = description != null && description.length() > 100
                ? description.substring(0, 100)
                : (description != null ? description : "Kimwanyi SACCO Payment");

        // Build billing address sub-object
        Map<String, Object> billing = Map.of(
                "email_address", email    != null ? email : "member@kimwanyisacco.com",
                "phone_number",  phone    != null ? phone : "",
                "country_code",  "UG",
                "first_name",    firstName != null ? firstName : "Member",
                "last_name",     lastName  != null ? lastName  : ""
        );

        Map<String, Object> payload = Map.of(
                "id",              merchantReference,
                "currency",        currency,
                "amount",          amount,
                "description",     safeDesc,
                "callback_url",    callbackUrl,
                "notification_id", ipnId,
                "branch",          branch != null ? branch : businessName,
                "billing_address", billing
        );

        String body = toJson(payload);
        log.info("Submitting PesaPal order: merchant_ref={}, amount={} {}",
                 merchantReference, amount, currency);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/Transactions/SubmitOrderRequest"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + getBearerToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = executeRequest(request, "Transactions/SubmitOrderRequest");

        if (response.statusCode() != 200) {
            log.error("PesaPal SubmitOrderRequest failed — HTTP {}: {}",
                      response.statusCode(), response.body());
            throw new PaymentProcessingException(
                    "Payment gateway rejected the order. Please try again.");
        }

        PesapalOrderResponse orderResp = parseJson(response.body(), PesapalOrderResponse.class);

        if (orderResp.getError() != null && orderResp.getError().getMessage() != null) {
            throw new PaymentProcessingException(
                    "PesaPal order error: " + orderResp.getError().getMessage());
        }

        if (orderResp.getRedirectUrl() == null || orderResp.getRedirectUrl().isEmpty()) {
            throw new PaymentProcessingException(
                    "PesaPal returned no redirect URL. Check gateway credentials.");
        }

        log.info("PesaPal order submitted — order_tracking_id={}, redirect_url={}",
                 orderResp.getOrderTrackingId(), orderResp.getRedirectUrl());
        return orderResp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET TRANSACTION STATUS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Polls PesaPal for the current status of a payment.
     *
     * <p>Called by:
     * <ul>
     *   <li>The IPN handler (PesaPal notifies us)</li>
     *   <li>The frontend poll endpoint {@code GET /api/v1/payments/verify/{ref}}</li>
     * </ul>
     *
     * @param orderTrackingId PesaPal's own tracking id (from submitOrder response)
     * @return PesapalTransactionStatus with statusCode 0/1/2/3
     */
    public PesapalTransactionStatus getTransactionStatus(String orderTrackingId) {
        log.debug("Polling PesaPal transaction status: orderTrackingId={}", orderTrackingId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl
                        + "/api/Transactions/GetTransactionStatus?orderTrackingId="
                        + orderTrackingId))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + getBearerToken())
                .GET()
                .build();

        HttpResponse<String> response = executeRequest(request, "Transactions/GetTransactionStatus");

        if (response.statusCode() != 200) {
            log.warn("GetTransactionStatus non-200 for {}: HTTP {}",
                     orderTrackingId, response.statusCode());
            // Return a neutral "not yet completed" status rather than throwing
            PesapalTransactionStatus neutral = new PesapalTransactionStatus();
            return neutral;
        }

        return parseJson(response.body(), PesapalTransactionStatus.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Executes an HTTP request and returns the response; wraps checked exceptions. */
    private HttpResponse<String> executeRequest(HttpRequest request, String endpoint) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("HTTP call to PesaPal {} failed: {}", endpoint, e.getMessage());
            throw new PaymentProcessingException(
                    "Could not connect to payment gateway. Please try again later.");
        }
    }

    /** Serializes a map to a compact JSON string. */
    private String toJson(Map<?, ?> map) {
        try {
            return json.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    /** Deserializes a JSON string into the given class. */
    private <T> T parseJson(String body, Class<T> clazz) {
        try {
            return json.readValue(body, clazz);
        } catch (Exception e) {
            log.error("Failed to parse PesaPal response into {}: {}", clazz.getSimpleName(), body);
            throw new PaymentProcessingException(
                    "Unexpected response from payment gateway. Please try again.");
        }
    }
}
