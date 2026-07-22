package com.kimwanyisacco.web;

import org.springframework.stereotype.Component;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Application-scoped helper bean that provides date-formatting utilities
 * for JSF views.
 *
 * <p>Background: JSF's {@code <f:convertDateTime>} uses the legacy
 * {@link java.text.DateFormat} API which only accepts {@link java.util.Date}.
 * It cannot format {@link LocalDate} (or any {@code java.time.*} type) and
 * throws {@code IllegalArgumentException: Cannot format given Object as a Date}.
 *
 * <p>This bean is the zero-dependency fix: expose a single {@code format}
 * method that views call via EL instead of {@code <f:convertDateTime>}:
 * <pre>
 *   #{dateUtil.format(loan.dueDate)}
 * </pre>
 */
@Component("dateUtil")
@ApplicationScoped
public class DateUtil {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Formats a {@link LocalDate} as {@code dd MMM yyyy} (e.g. 21 Jul 2026).
     * Returns an empty string when {@code date} is {@code null}.
     */
    public String format(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY);
    }
}
