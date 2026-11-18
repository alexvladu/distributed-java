package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MarketControlTest {
    private final MarketControl market = new MarketControl();

    @Test
    void listsTheCatalogueSortedBySymbol() {
        assertEquals(List.of("AAPL", "AMZN", "MSFT", "NVDA", "TSLA"),
                market.quotes().stream().map(StockQuote::symbol).toList());
    }

    @Test
    void findsSymbolsCaseInsensitivelyAndRejectsUnknownOnes() {
        assertEquals("Apple Inc.", market.quote(" aapl ").orElseThrow().name());
        assertTrue(market.quote("IBM").isEmpty());
        assertTrue(market.quote(null).isEmpty());
        assertTrue(market.quote(" ").isEmpty());
    }
}
