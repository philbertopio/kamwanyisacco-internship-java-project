package com.kimwanyisacco.exception;

import com.kimwanyisacco.exception.SaccoException;

/** Thrown when an actor attempts an action their role does not permit. */
public class UnauthorizedActionException extends SaccoException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
