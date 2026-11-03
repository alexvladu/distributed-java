package com.example.asset.control;

import jakarta.ejb.ApplicationException;

/** A dependency failed in a way the user can retry; the message is safe to show. */
@ApplicationException
public class UnavailableException extends RuntimeException {
    public UnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
