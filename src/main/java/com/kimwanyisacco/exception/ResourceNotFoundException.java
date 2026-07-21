package com.kimwanyisacco.exception;

public class ResourceNotFoundException extends SaccoException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier));
    }
}
