package com.example.asset.control;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/** The tradable stocks and their fixed prices. Immutable, so no container locks. */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class MarketControl {
    private static final List<StockQuote> CATALOGUE = List.of(
            new StockQuote("AAPL", "Apple Inc.", new BigDecimal("190.25")),
            new StockQuote("AMZN", "Amazon.com Inc.", new BigDecimal("185.00")),
            new StockQuote("MSFT", "Microsoft Corporation", new BigDecimal("425.50")),
            new StockQuote("NVDA", "NVIDIA Corporation", new BigDecimal("138.75")),
            new StockQuote("TSLA", "Tesla Inc.", new BigDecimal("245.10")));

    private final Map<String, StockQuote> quotes = new TreeMap<>();

    public MarketControl() {
        this(CATALOGUE);
    }

    MarketControl(List<StockQuote> catalogue) {
        catalogue.forEach(quote -> quotes.put(quote.symbol(), quote));
    }

    public Optional<StockQuote> quote(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(quotes.get(symbol.trim().toUpperCase(Locale.ROOT)));
    }

    /** Sorted by symbol. */
    public List<StockQuote> quotes() {
        return List.copyOf(quotes.values());
    }
}
