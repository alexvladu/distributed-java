package com.example.asset.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StockCompanyJson {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StockCompanyJson() {
    }

    public static StockCompany read(String value) {
        try {
            return OBJECT_MAPPER.readValue(value, StockCompany.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid StockCompany JSON", exception);
        }
    }
}
