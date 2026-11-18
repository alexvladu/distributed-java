package com.example.asset.control;

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
    public record Health(String status, Map<String, String> components) {
    }

    @EJB
    private Database database;

    public Health check() {
        Map<String, String> components = new LinkedHashMap<>();
        database.ping().forEach((name, reachable) ->
                components.put(name, Boolean.TRUE.equals(reachable) ? "UP" : "DOWN"));
        boolean healthy = components.values().stream().allMatch("UP"::equals);
        return new Health(healthy ? "UP" : "DEGRADED", components);
    }
}
