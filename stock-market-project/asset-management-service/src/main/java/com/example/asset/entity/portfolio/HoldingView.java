package com.example.asset.entity.portfolio;

import java.math.BigDecimal;

/** A position valued at the market price; price fields are null when the symbol is no longer traded. */
public record HoldingView(String symbol, String name, BigDecimal quantity, BigDecimal averageCost,
        BigDecimal costBasis, BigDecimal price, BigDecimal marketValue, BigDecimal unrealizedProfit) {
}
