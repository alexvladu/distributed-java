package com.example.asset.control;

import java.math.BigDecimal;
import java.util.List;

public record AccountSnapshot(long portfolioId, String portfolioName, BigDecimal cashBalance,
        BigDecimal investedValue, BigDecimal costBasis, BigDecimal unrealizedProfit, BigDecimal totalValue,
        List<HoldingView> holdings) {
}
