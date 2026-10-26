package com.example.asset.control;

import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** In-memory view of the live market, fed by the Kafka boundary. */
public final class MarketControl {
    static final int HISTORY_SIZE = 40;
    private static final System.Logger LOG = System.getLogger(MarketControl.class.getName());

    private final ConcurrentMap<String, StockQuote> quotes = new ConcurrentHashMap<>();
    private final List<Consumer<StockQuote>> listeners = new CopyOnWriteArrayList<>();
    private volatile Instant lastUpdateReceivedAt;

    /** Applies a price update; updates older than the current quote are ignored. */
    public void apply(String symbol, String name, BigDecimal price, Instant timestamp) {
        String key = normalize(symbol);
        Instant effectiveTimestamp = timestamp == null ? Instant.now() : timestamp;
        AtomicBoolean changed = new AtomicBoolean();
        StockQuote quote = quotes.compute(key, (ignored, previous) -> {
            if (previous != null && effectiveTimestamp.isBefore(previous.timestamp())) {
                return previous;
            }
            changed.set(true);
            return previous == null
                    ? StockQuote.first(key, name, price, effectiveTimestamp)
                    : previous.next(name, price, effectiveTimestamp, HISTORY_SIZE);
        });
        if (!changed.get()) {
            return;
        }
        lastUpdateReceivedAt = Instant.now();
        for (Consumer<StockQuote> listener : listeners) {
            try {
                listener.accept(quote);
            } catch (RuntimeException exception) {
                LOG.log(Level.WARNING, "Market listener failed", exception);
            }
        }
    }

    public Optional<StockQuote> quote(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(quotes.get(normalize(symbol)));
    }

    public List<StockQuote> quotes() {
        return quotes.values().stream().sorted(Comparator.comparing(StockQuote::symbol)).toList();
    }

    public Instant lastUpdateReceivedAt() {
        return lastUpdateReceivedAt;
    }

    public void subscribe(Consumer<StockQuote> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<StockQuote> listener) {
        listeners.remove(listener);
    }

    private static String normalize(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
