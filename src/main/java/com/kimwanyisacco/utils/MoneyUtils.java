package com.kimwanyisacco.utils;

import com.kimwanyisacco.utils.SaccoConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralised money rounding so every BigDecimal calculation in the system
 * uses the same scale and rounding mode - avoids subtle discrepancies
 * between, e.g., interest calculations done in different services.
 */
public final class MoneyUtils {

    private static final int SCALE = 2;

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFlatLoanInterest(BigDecimal principal) {
        return round(principal.multiply(SaccoConstants.LOAN_FLAT_INTEREST_RATE));
    }

    public static BigDecimal calculateMonthlySavingsInterest(BigDecimal balance) {
        BigDecimal monthlyRate = SaccoConstants.ANNUAL_SAVINGS_INTEREST_RATE
                .divide(BigDecimal.valueOf(SaccoConstants.MONTHS_PER_YEAR), 10, RoundingMode.HALF_UP);
        return round(balance.multiply(monthlyRate));
    }

    public static BigDecimal maxLoanAmount(BigDecimal savingsBalance) {
        return round(savingsBalance.multiply(SaccoConstants.MAX_LOAN_MULTIPLIER));
    }
}
