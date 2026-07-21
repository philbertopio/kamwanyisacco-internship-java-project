package com.kimwanyisacco.exception;

/**
 * Generic business-rule violation, e.g.:
 * - member already has an active loan
 * - loan amount exceeds 3x savings balance
 * - non-admin attempting to approve/reject a loan
 */
public class BusinessRuleViolationException extends SaccoException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
