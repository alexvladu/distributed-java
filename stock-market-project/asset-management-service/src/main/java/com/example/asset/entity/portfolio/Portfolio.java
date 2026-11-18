package com.example.asset.entity.portfolio;

import java.math.BigDecimal;
import java.time.Instant;

public record Portfolio(long portfolioId, long userId, String portfolioName, BigDecimal cashBalance,
        Instant createdAt) {
}
