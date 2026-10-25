package com.example.asset.entity.portfolio;

import java.time.LocalDateTime;

public record Portfolio(long portfolioId, long userId, String portfolioName, LocalDateTime createdAt) {
}
