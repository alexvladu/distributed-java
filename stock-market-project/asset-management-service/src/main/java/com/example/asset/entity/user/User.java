package com.example.asset.entity.user;

import java.time.OffsetDateTime;

public record User(long userId, String username, String email, OffsetDateTime createdAt) {
}
