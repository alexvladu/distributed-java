package com.example.asset.entity.trade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Trade(long tradeId, long portfolioId, String symbol, String side,
        BigDecimal quantity, OffsetDateTime executedAt) {
}
