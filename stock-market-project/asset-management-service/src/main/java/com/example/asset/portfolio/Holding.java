package com.example.asset.portfolio;

import java.math.BigDecimal;

public record Holding(String symbol, String name, BigDecimal quantity) {
}
