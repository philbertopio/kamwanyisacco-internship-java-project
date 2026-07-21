package com.kimwanyisacco.exception;

public class SaccoException extends RuntimeException {

    public SaccoException(String message) {
        super(message);
    }

    public SaccoException(String message, Throwable cause) {
        super(message, cause);
    }
}