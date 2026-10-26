package com.example.asset.control;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class HealthControl {
    private static final Duration MARKET_STALE_AFTER = Duration.ofMinutes(2);

    public record Health(String status, Map<String, String> components) {
    }

    private final Supplier<Map<String, Boolean>> databasePing;
    private final MarketControl market;

    public HealthControl(Supplier<Map<String, Boolean>> databasePing, MarketControl market) {
        this.databasePing = databasePing;
        this.market = market;
    }

    public Health check() {
        Map<String, String> components = new LinkedHashMap<>();
        databasePing.get().forEach((name, reachable) ->
                components.put(name, Boolean.TRUE.equals(reachable) ? "UP" : "DOWN"));
        Instant lastUpdate = market.lastUpdateReceivedAt();
        String marketStatus;
        if (lastUpdate == null) {
            marketStatus = "WAITING";
        } else if (lastUpdate.isBefore(Instant.now().minus(MARKET_STALE_AFTER))) {
            marketStatus = "STALE";
        } else {
            marketStatus = "UP";
        }
        components.put("market", marketStatus);
        boolean healthy = components.values().stream().allMatch("UP"::equals);
        return new Health(healthy ? "UP" : "DEGRADED", components);
    }
}
