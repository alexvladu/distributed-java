package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MarketControlTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final MarketControl market = new MarketControl();

    @Test
    void tracksThePreviousPriceAndABoundedHistory() {
        int updates = MarketControl.HISTORY_SIZE + 5;
        for (int index = 0; index < updates; index++) {
            market.apply("aapl", "Apple Inc.", BigDecimal.valueOf(100 + index), START.plusSeconds(index));
        }

        StockQuote quote = market.quote("AAPL").orElseThrow();
        assertEquals("AAPL", quote.symbol());
        assertEquals(0, BigDecimal.valueOf(100 + updates - 1).compareTo(quote.price()));
        assertEquals(0, BigDecimal.valueOf(100 + updates - 2).compareTo(quote.previousPrice()));
        assertEquals(MarketControl.HISTORY_SIZE, quote.history().size());
        assertEquals(0, quote.price().compareTo(quote.history().get(quote.history().size() - 1)));
    }

    @Test
    void firstQuoteHasNoPreviousPrice() {
        market.apply("MSFT", "Microsoft", new BigDecimal("425.50"), START);

        StockQuote quote = market.quote("msft").orElseThrow();
        assertNull(quote.previousPrice());
        assertEquals(1, quote.history().size());
    }

    @Test
    void ignoresUpdatesOlderThanTheCurrentQuote() {
        List<StockQuote> notified = new ArrayList<>();
        market.subscribe(notified::add);
        market.apply("AAPL", "Apple Inc.", new BigDecimal("101"), START.plusSeconds(10));

        market.apply("AAPL", "Apple Inc.", new BigDecimal("99"), START);

        assertEquals(0, new BigDecimal("101").compareTo(market.quote("AAPL").orElseThrow().price()));
        assertEquals(1, notified.size());
    }

    @Test
    void aFailingListenerDoesNotStopTheOthers() {
        List<String> seen = new ArrayList<>();
        market.subscribe(quote -> {
            throw new IllegalStateException("listener failure");
        });
        market.subscribe(quote -> seen.add(quote.symbol()));

        market.apply("TSLA", "Tesla Inc.", new BigDecimal("245.10"), START);

        assertEquals(List.of("TSLA"), seen);
    }

    @Test
    void quotesAreSortedAndUnknownSymbolsAreEmpty() {
        market.apply("TSLA", "Tesla Inc.", BigDecimal.ONE, START);
        market.apply("AAPL", "Apple Inc.", BigDecimal.ONE, START);

        assertEquals(List.of("AAPL", "TSLA"), market.quotes().stream().map(StockQuote::symbol).toList());
        assertTrue(market.quote("NVDA").isEmpty());
        assertTrue(market.quote(null).isEmpty());
        assertTrue(market.quote(" ").isEmpty());
    }
}
