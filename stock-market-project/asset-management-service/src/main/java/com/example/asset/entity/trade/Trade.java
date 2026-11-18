package com.example.asset.entity.trade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** An execution. {@code price} is null only for trades recorded before prices were tracked. */
public record Trade(long tradeId, long portfolioId, String symbol, TradeSide side, BigDecimal quantity,
        BigDecimal price, OffsetDateTime executedAt) {
}
