package com.example.asset.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record AccountState(BigDecimal cashBalance, List<Holding> holdings) {
}
