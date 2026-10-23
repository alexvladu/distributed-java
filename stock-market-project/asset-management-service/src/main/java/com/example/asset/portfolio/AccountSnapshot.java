package com.example.asset.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record AccountSnapshot(
        BigDecimal cashBalance,
        BigDecimal investedValue,
        BigDecimal totalValue,
        List<Holding> holdings) {
}
