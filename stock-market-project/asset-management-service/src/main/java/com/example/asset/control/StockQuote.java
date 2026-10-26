package com.example.asset.control;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Latest price of a symbol plus a short, oldest-first price history for sparklines. */
public record StockQuote(String symbol, String name, BigDecimal price, BigDecimal previousPrice,
        Instant timestamp, List<BigDecimal> history) {

    public StockQuote {
        history = List.copyOf(history);
    }

    static StockQuote first(String symbol, String name, BigDecimal price, Instant timestamp) {
        return new StockQuote(symbol, name, price, null, timestamp, List.of(price));
    }

    StockQuote next(String newName, BigDecimal newPrice, Instant newTimestamp, int maxHistory) {
        List<BigDecimal> updated = new ArrayList<>(history);
        updated.add(newPrice);
        if (updated.size() > maxHistory) {
            updated = updated.subList(updated.size() - maxHistory, updated.size());
        }
        return new StockQuote(symbol, newName, newPrice, price, newTimestamp, updated);
    }
}
