package com.example.stock;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StockCompanyJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StockCompanyJson() {
    }

    public static String write(StockCompany company) {
        try {
            return OBJECT_MAPPER.writeValueAsString(company);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize StockCompany", exception);
        }
    }

    public static String writeAll(List<StockCompany> companies) {
        return companies.stream()
                .map(StockCompanyJson::write)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
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
