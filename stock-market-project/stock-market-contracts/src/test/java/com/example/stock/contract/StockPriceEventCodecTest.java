package com.example.stock.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class StockPriceEventCodecTest {
    @Test
    void roundTripsEveryField() {
        StockPriceEvent event = new StockPriceEvent("AAPL", "Apple \"Inc.\" \\ Co", new BigDecimal("190.25"), 12,
                Instant.parse("2026-01-02T03:04:05.123Z"));

        assertEquals(event, StockPriceEventCodec.decode(StockPriceEventCodec.encode(event)));
    }

    @Test
    void writesPricesWithoutScientificNotation() {
        String json = StockPriceEventCodec.encode(
                new StockPriceEvent("X", "X Corp", new BigDecimal("1E+3"), 1, null));

        assertTrue(json.contains("\"price\":1000"), json);
    }

    @Test
    void readsLegacyMessagesWithoutTimestampAndIgnoresUnknownFields() {
        StockPriceEvent event = StockPriceEventCodec.decode(
                "{\"symbol\":\"MSFT\",\"name\":\"Microsoft\",\"price\":425.50,\"quantity\":8,\"extra\":true}");

        assertEquals("MSFT", event.symbol());
        assertEquals(0, new BigDecimal("425.50").compareTo(event.price()));
        assertEquals(8, event.quantity());
        assertNull(event.timestamp());
    }

    @Test
    void rejectsMalformedOrInvalidMessages() {
        assertThrows(IllegalArgumentException.class, () -> StockPriceEventCodec.decode(null));
        assertThrows(IllegalArgumentException.class, () -> StockPriceEventCodec.decode(""));
        assertThrows(IllegalArgumentException.class, () -> StockPriceEventCodec.decode("null"));
        assertThrows(IllegalArgumentException.class, () -> StockPriceEventCodec.decode("not json"));
        assertThrows(IllegalArgumentException.class,
                () -> StockPriceEventCodec.decode("{\"symbol\":\"\",\"name\":\"X\",\"price\":1}"));
        assertThrows(IllegalArgumentException.class,
                () -> StockPriceEventCodec.decode("{\"symbol\":\"X\",\"name\":\"X\",\"price\":-1}"));
        assertThrows(IllegalArgumentException.class,
                () -> StockPriceEventCodec.decode("{\"symbol\":\"X\",\"name\":\"X\"}"));
    }
}
