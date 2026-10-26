package com.example.asset.boundary.http;

import java.util.OptionalLong;

import com.example.asset.entity.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** The server-side session is the only source of the authenticated user's identity. */
public final class SessionSupport {
    private static final String USER_ID = "authenticatedUserId";
    private static final String USERNAME = "authenticatedUsername";

    private SessionSupport() {
    }

    /** Starts a fresh session so a pre-login session ID can never be reused (session fixation). */
    public static void login(HttpServletRequest request, User user) {
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, user.userId());
        session.setAttribute(USERNAME, user.username());
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static OptionalLong userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return OptionalLong.empty();
        }
        try {
            return session.getAttribute(USER_ID) instanceof Long id ? OptionalLong.of(id) : OptionalLong.empty();
        } catch (IllegalStateException invalidated) {
            return OptionalLong.empty();
        }
    }

    public static long requireUserId(HttpServletRequest request) {
        return userId(request).orElseThrow(UnauthorizedException::new);
    }

    public static void updateUsername(HttpServletRequest request, String username) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(USERNAME, username);
        }
    }
}
