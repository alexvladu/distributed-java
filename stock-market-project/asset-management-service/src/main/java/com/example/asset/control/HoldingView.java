package com.example.asset.control;

import java.math.BigDecimal;

/** A position valued at the latest market price; price fields are null while the symbol has no quote. */
public record HoldingView(String symbol, String name, BigDecimal quantity, BigDecimal averageCost,
        BigDecimal costBasis, BigDecimal price, BigDecimal marketValue, BigDecimal unrealizedProfit) {
}
