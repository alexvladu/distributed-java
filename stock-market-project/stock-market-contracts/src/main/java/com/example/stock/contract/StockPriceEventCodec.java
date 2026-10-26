package com.example.stock.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class StockPriceEventCodec {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    private StockPriceEventCodec() {
    }

    public static String encode(StockPriceEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode stock price event", exception);
        }
    }

    public static StockPriceEvent decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Stock price message is empty");
        }
        StockPriceEvent event;
        try {
            event = MAPPER.readValue(json, StockPriceEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid stock price message: " + exception.getOriginalMessage(),
                    exception);
        }
        if (event == null) {
            throw new IllegalArgumentException("Stock price message is empty");
        }
        return event;
    }
}
