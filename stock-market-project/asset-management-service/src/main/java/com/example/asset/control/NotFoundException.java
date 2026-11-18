package com.example.asset.control;

import jakarta.ejb.ApplicationException;

/** The resource does not exist or is not visible to the current user. */
@ApplicationException
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
