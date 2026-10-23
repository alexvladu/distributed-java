package com.example.stock;

import java.math.BigDecimal;
import java.util.List;

public final class StockCompanyJson {
    private StockCompanyJson() {
    }

    public static String write(StockCompany company) {
        return "{\"symbol\":\"" + escape(company.symbol()) + "\","
                + "\"name\":\"" + escape(company.name()) + "\","
                + "\"price\":" + company.price().toPlainString() + ","
                + "\"quantity\":" + company.quantity() + "}";
    }

    public static String writeAll(List<StockCompany> companies) {
        return companies.stream()
                .map(StockCompanyJson::write)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static StockCompany fromParameters(String symbol, String name, String price, String quantity) {
        if (symbol == null || symbol.isBlank() || name == null || name.isBlank()
                || price == null || quantity == null) {
            throw new IllegalArgumentException("symbol, name, price, and quantity are required");
        }

        try {
            BigDecimal parsedPrice = new BigDecimal(price);
            long parsedQuantity = Long.parseLong(quantity);
            if (parsedPrice.signum() < 0 || parsedQuantity < 0) {
                throw new IllegalArgumentException("price and quantity must not be negative");
            }
            return new StockCompany(symbol.trim(), name.trim(), parsedPrice, parsedQuantity);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("price must be decimal and quantity must be an integer", exception);
        }
    }
}
