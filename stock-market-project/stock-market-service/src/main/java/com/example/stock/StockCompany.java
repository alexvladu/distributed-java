package com.example.stock;

import java.math.BigDecimal;

public record StockCompany(String symbol, String name, BigDecimal price, long quantity) {
}
