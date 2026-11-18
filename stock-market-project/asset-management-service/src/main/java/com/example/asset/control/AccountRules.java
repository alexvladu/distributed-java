package com.example.asset.control;

import java.util.regex.Pattern;

/** Validation rules shared by registration and profile updates. */
public final class AccountRules {
    public static final int MIN_PASSWORD_LENGTH = 10;
    public static final int MAX_PASSWORD_LENGTH = 200;
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,80}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MAX_EMAIL_LENGTH = 255;

    private AccountRules() {
    }

    public static String username(String value) {
        String username = value == null ? "" : value.trim();
        if (!USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must contain 3-80 letters, numbers, or underscores");
        }
        return username;
    }

    public static String email(String value) {
        String email = value == null ? "" : value.trim();
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email is required");
        }
        return email;
    }

    public static void password(String value) {
        if (value == null || value.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (value.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain at most " + MAX_PASSWORD_LENGTH + " characters");
        }
    }
}
