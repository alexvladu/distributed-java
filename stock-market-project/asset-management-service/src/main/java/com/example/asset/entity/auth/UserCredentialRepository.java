package com.example.asset.entity.auth;

import java.util.Optional;

public interface UserCredentialRepository {
    void create(long userId, String passwordHash);
    Optional<Credential> findByUsername(String username);
    void recordFailedAttempt(long userId, int failedAttempts);
    void recordSuccessfulLogin(long userId);

    record Credential(long userId, String username, String passwordHash, int failedAttempts,
            java.time.OffsetDateTime lockedUntil) {
    }
}
