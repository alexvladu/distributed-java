package com.example.asset.control;

import java.math.BigDecimal;

/** A tradable stock and its price. */
public record StockQuote(String symbol, String name, BigDecimal price) {
}
