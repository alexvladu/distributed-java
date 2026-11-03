package com.example.asset.entity.auth;

import java.time.OffsetDateTime;

public record Credential(long userId, String username, String passwordHash, int failedAttempts,
        OffsetDateTime lockedUntil) {
}
