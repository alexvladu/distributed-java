package com.example.asset.control;

import com.example.asset.entity.auth.UserCredentialRepository;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AuthControl {
    private static final int ITERATIONS = 210_000;
    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final SecureRandom random = new SecureRandom();

    public AuthControl(UserRepository users, UserCredentialRepository credentials) {
        this.users = users; this.credentials = credentials;
    }

    public User register(String username, String email, String password) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,80}")) {
            throw new IllegalArgumentException("Username must contain 3-80 letters, numbers, or underscores");
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException("Password must contain at least 10 characters");
        }
        if (users.findAll().stream().anyMatch(user -> user.username().equalsIgnoreCase(username))) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = users.create(username, email);
        credentials.create(user.userId(), hash(password));
        return user;
    }

    public User authenticate(String username, String password) {
        var credential = credentials.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Invalid username or password"));
        if (credential.lockedUntil() != null && credential.lockedUntil().isAfter(java.time.OffsetDateTime.now())) {
            throw new SecurityException("Account is temporarily locked");
        }
        if (!matches(password, credential.passwordHash())) {
            credentials.recordFailedAttempt(credential.userId(), credential.failedAttempts() + 1);
            throw new SecurityException("Invalid username or password");
        }
        credentials.recordSuccessfulLogin(credential.userId());
        return users.findById(credential.userId()).orElseThrow(() -> new SecurityException("Invalid username or password"));
    }

    private String hash(String password) {
        byte[] salt = new byte[16]; random.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    private boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
            return MessageDigest.isEqual(actual, Base64.getDecoder().decode(parts[3]));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, 256)).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        }
    }
}
