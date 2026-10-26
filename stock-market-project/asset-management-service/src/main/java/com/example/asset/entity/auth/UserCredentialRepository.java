package com.example.asset.entity.auth;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.example.asset.entity.user.User;

public interface UserCredentialRepository {
    /** Creates the user and its credentials atomically. */
    User createAccount(String username, String email, String passwordHash);

    Optional<Credential> findByUsername(String username);

    void recordFailedAttempt(long userId);

    void recordSuccessfulLogin(long userId);

    record Credential(long userId, String username, String passwordHash, int failedAttempts,
            OffsetDateTime lockedUntil) {
    }
}
