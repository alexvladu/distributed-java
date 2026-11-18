package com.example.asset.entity.auth;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** PostgreSQL {@code user_credentials} row; its primary key is also the foreign key to {@code users}. */
@Entity
@Table(name = "user_credentials")
public class CredentialEntity {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CredentialEntity() {
    }

    public CredentialEntity(long userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.updatedAt = now();
    }

    /** Locks the account for 15 minutes on the fifth consecutive failure. */
    public void recordFailedAttempt() {
        OffsetDateTime now = now();
        failedAttempts++;
        lockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
        updatedAt = now;
    }

    public void recordSuccessfulLogin() {
        OffsetDateTime now = now();
        failedAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public Credential toCredential(String username) {
        return new Credential(userId, username, passwordHash, failedAttempts, lockedUntil);
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
