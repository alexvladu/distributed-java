package com.example.stock.contract;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Message published on the stock price topic, keyed by symbol. {@code timestamp} is optional so
 * messages produced before it was introduced can still be read.
 */
public record StockPriceEvent(String symbol, String name, BigDecimal price, long quantity, Instant timestamp) {
    public static final String DEFAULT_TOPIC = "stock-prices";

    public StockPriceEvent {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must be zero or positive");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
    }
}
