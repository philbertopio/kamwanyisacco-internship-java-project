package com.kimwanyisacco.exception;

/**
 * Thrown when a withdrawal would breach the available balance or the
 * mandatory minimum savings balance of UGX 20,000.
 * Directly fixes: "a member withdraws more than they have because the
 * cashier misread the balance".
 */
public class InsufficientBalanceException extends SaccoException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
