package com.example.asset.control;

import java.lang.System.Logger.Level;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.example.asset.entity.auth.UserCredentialRepository;
import com.example.asset.entity.auth.UserCredentialRepository.Credential;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;

public final class AuthControl {
    private static final System.Logger LOG = System.getLogger(AuthControl.class.getName());
    private static final int ITERATIONS = 210_000;
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final PortfolioControl portfolios;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthControl(UserRepository users, UserCredentialRepository credentials, PortfolioControl portfolios) {
        this.users = users;
        this.credentials = credentials;
        this.portfolios = portfolios;
        this.dummyHash = hash("timing-equalization-only");
    }

    public User register(String username, String email, String password) {
        String validUsername = AccountRules.username(username);
        String validEmail = AccountRules.email(email);
        AccountRules.password(password);
        if (users.existsByUsername(validUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = credentials.createAccount(validUsername, validEmail, hash(password));
        try {
            portfolios.createDefault(user.userId());
        } catch (RuntimeException exception) {
            // The account is usable without it; a portfolio can be created later from the UI.
            LOG.log(Level.WARNING, "Could not create the default portfolio for user " + user.userId(), exception);
        }
        return user;
    }

    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        Optional<Credential> found = credentials.findByUsername(username.trim());
        if (found.isEmpty()) {
            // Spend the same hashing time as a real check so usernames cannot be probed by timing.
            matches(password, dummyHash);
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        Credential credential = found.get();
        if (credential.lockedUntil() != null
                && credential.lockedUntil().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new SecurityException("Account is temporarily locked after too many failed attempts");
        }
        if (!matches(password, credential.passwordHash())) {
            credentials.recordFailedAttempt(credential.userId());
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        credentials.recordSuccessfulLogin(credential.userId());
        return users.findById(credential.userId())
                .orElseThrow(() -> new SecurityException(INVALID_CREDENTIALS));
    }

    String hash(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
            return MessageDigest.isEqual(actual, Base64.getDecoder().decode(parts[3]));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, 256))
                    .getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        }
    }
}
