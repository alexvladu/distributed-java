package com.example.asset.control;

/** The resource does not exist or is not visible to the current user. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
