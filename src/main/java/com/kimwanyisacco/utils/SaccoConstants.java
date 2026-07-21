package com.kimwanyisacco.utils;

import java.math.BigDecimal;

public final class SaccoConstants {

    private SaccoConstants() {
    }

    public static final BigDecimal MIN_SAVINGS_BALANCE = new BigDecimal("20000.00");

    /** 5% per annum, applied monthly -> 5% / 12 each month. */
    public static final BigDecimal ANNUAL_SAVINGS_INTEREST_RATE = new BigDecimal("0.05");
    public static final int MONTHS_PER_YEAR = 12;

    /** Flat 10% of principal, charged once at loan issuance. */
    public static final BigDecimal LOAN_FLAT_INTEREST_RATE = new BigDecimal("0.10");

    /** Maximum loan = 3x the member's current savings balance. */
    public static final BigDecimal MAX_LOAN_MULTIPLIER = new BigDecimal("3");
}
