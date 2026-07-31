package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.request.MemberRegistrationRequest;
import com.kimwanyisacco.exception.DuplicateResourceException;
import com.kimwanyisacco.service.MemberService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.regex.Pattern;

/**
 * Backing bean for the member registration form.
 * Performs server-side validation before delegating to MemberService.
 * All messages shown to the user are business-level; no stack traces or
 * technical detail are ever forwarded to the view.
 */
@Getter
@Setter
@Component("registrationBean")
@Scope("request")
public class RegistrationBean {

    private static final Logger log = LoggerFactory.getLogger(RegistrationBean.class);

    // ── Patterns must mirror those in MemberRegistrationRequest and register.xhtml ──
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,20}$");
    private static final Pattern USERNAME_PATTERN    = Pattern.compile("^[A-Za-z0-9_\\-]{4,50}$");
    private static final Pattern EMAIL_PATTERN       = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PHONE_PATTERN       = Pattern.compile("^\\+?[0-9]{9,15}$");
    private static final Pattern PASSWORD_PATTERN    = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$");

    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String nationalId;
    private String phone;
    private String address;

    private final MemberService memberService;

    @Autowired
    public RegistrationBean(MemberService memberService) {
        this.memberService = memberService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Action
    // ─────────────────────────────────────────────────────────────────────────

    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Server-side validation (defence-in-depth: client JS may be disabled)
        if (hasValidationErrors(ctx)) {
            return null;
        }

        try {
            MemberRegistrationRequest request = new MemberRegistrationRequest(
                    trim(username), trim(email), password,
                    trim(fullName), trim(nationalId), trim(phone),
                    address != null ? address.trim() : null);

            memberService.registerMember(request);

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Account created successfully. Please log in.", null));
            return "login?faces-redirect=true";

        } catch (DuplicateResourceException ex) {
            // Business-level message – safe to surface directly
            log.warn("Registration rejected – duplicate resource: {}", ex.getMessage());
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ex.getMessage(), null));
            return null;

        } catch (Exception ex) {
            // Log the technical detail server-side; show only a generic message
            log.error("Unexpected error during member registration for username '{}': {}",
                    username, ex.getMessage(), ex);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "We could not complete your registration at this time. "
                            + "Please try again or contact support if the problem persists.", null));
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates all fields and posts per-field FacesMessages.
     *
     * @return {@code true} if at least one validation error was found
     */
    private boolean hasValidationErrors(FacesContext ctx) {
        boolean hasError = false;

        // Full Name
        if (isBlank(fullName)) {
            addError(ctx, "registerForm:fullName", "Full name is required.");
            hasError = true;
        } else if (fullName.trim().length() < 2 || fullName.trim().length() > 150) {
            addError(ctx, "registerForm:fullName", "Full name must be between 2 and 150 characters.");
            hasError = true;
        }

        // National ID
        if (isBlank(nationalId)) {
            addError(ctx, "registerForm:nationalId", "National ID number is required.");
            hasError = true;
        } else if (!NATIONAL_ID_PATTERN.matcher(nationalId.trim()).matches()) {
            addError(ctx, "registerForm:nationalId",
                    "National ID must be 6–20 alphanumeric characters (letters and digits only, no spaces or symbols).");
            hasError = true;
        }

        // Username
        if (isBlank(username)) {
            addError(ctx, "registerForm:username", "Username is required.");
            hasError = true;
        } else if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            addError(ctx, "registerForm:username",
                    "Username must be 4–50 characters and may only contain letters, digits, underscores, or hyphens.");
            hasError = true;
        }

        // Email
        if (isBlank(email)) {
            addError(ctx, "registerForm:email", "Email address is required.");
            hasError = true;
        } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            addError(ctx, "registerForm:email",
                    "Please enter a valid email address (e.g. name@example.com).");
            hasError = true;
        }

        // Password
        if (isBlank(password)) {
            addError(ctx, "registerForm:password", "Password is required.");
            hasError = true;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            addError(ctx, "registerForm:password",
                    "Password must be at least 8 characters and include an uppercase letter, "
                            + "a lowercase letter, a number, and a special character.");
            hasError = true;
        }

        // Confirm Password
        if (isBlank(confirmPassword)) {
            addError(ctx, "registerForm:confirmPassword", "Please confirm your password.");
            hasError = true;
        } else if (password != null && !password.equals(confirmPassword)) {
            addError(ctx, "registerForm:confirmPassword", "Passwords do not match. Please re-enter both passwords.");
            hasError = true;
        }

        // Phone
        if (isBlank(phone)) {
            addError(ctx, "registerForm:phone", "Phone number is required.");
            hasError = true;
        } else if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            addError(ctx, "registerForm:phone",
                    "Please enter a valid phone number (e.g. +254712345678 or 0712345678).");
            hasError = true;
        }

        return hasError;
    }

    private static void addError(FacesContext ctx, String clientId, String message) {
        ctx.addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
