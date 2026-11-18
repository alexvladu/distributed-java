package com.example.asset.boundary.http;

/** The request has no authenticated session. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Authentication required");
    }
}
