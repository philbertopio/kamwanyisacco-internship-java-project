package com.kimwanyisacco.exception;

import com.kimwanyisacco.exception.SaccoException;

/** Thrown when a payment cannot be processed - failed gateway call, ownership mismatch, invalid card, etc. */
public class PaymentProcessingException extends SaccoException {

    public PaymentProcessingException(String message) {
        super(message);
    }
}
