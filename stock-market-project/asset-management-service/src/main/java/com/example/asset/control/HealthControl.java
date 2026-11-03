package com.example.asset.control;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.asset.control.repository.Database;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class HealthControl {
    private static final Duration MARKET_STALE_AFTER = Duration.ofMinutes(2);

    public record Health(String status, Map<String, String> components) {
    }

    @EJB
    private Database database;
    @EJB
    private MarketControl market;

    public Health check() {
        Map<String, String> components = new LinkedHashMap<>();
        database.ping().forEach((name, reachable) ->
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
