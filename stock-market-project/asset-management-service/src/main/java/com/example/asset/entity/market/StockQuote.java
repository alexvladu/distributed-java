package com.example.asset.entity.market;

import java.math.BigDecimal;

/** A tradable stock and its price. */
public record StockQuote(String symbol, String name, BigDecimal price) {
}
