package com.example.asset.control;

/** A dependency failed in a way the user can retry; the message is safe to show. */
public class UnavailableException extends RuntimeException {
    public UnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
