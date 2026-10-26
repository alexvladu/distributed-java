package com.example.asset.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AppConfigTest {
    private static final Map<String, String> SECRETS = Map.of(
            "POSTGRES_PASSWORD", "pg-secret",
            "SQLSERVER_PASSWORD", "mssql-secret",
            "ORACLE_APP_PASSWORD", "oracle-secret");

    @Test
    void failsFastListingEveryMissingSecret() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.from(Map.<String, String>of()::get));

        assertTrue(error.getMessage().contains("POSTGRES_PASSWORD"), error.getMessage());
        assertTrue(error.getMessage().contains("SQLSERVER_PASSWORD"), error.getMessage());
        assertTrue(error.getMessage().contains("ORACLE_APP_PASSWORD"), error.getMessage());
    }

    @Test
    void usesLocalDefaultsForEverythingExceptSecrets() {
        AppConfig config = AppConfig.from(SECRETS::get);

        assertEquals("jdbc:postgresql://localhost:5432/stock_market", config.postgres().url());
        assertEquals("stock_app", config.postgres().username());
        assertEquals("pg-secret", config.postgres().password());
        assertEquals("stock_app", config.sqlServer().username());
        assertEquals("mssql-secret", config.sqlServer().password());
        assertEquals("oracle-secret", config.oracle().password());
        assertEquals("localhost:9092", config.kafkaBootstrapServers());
        assertEquals("stock-prices", config.kafkaTopic());
    }

    @Test
    void acceptsTheLegacyOracleUserVariable() {
        Map<String, String> environment = new java.util.HashMap<>(SECRETS);
        environment.put("ORACLE_USER", "legacy_user");

        assertEquals("legacy_user", AppConfig.from(environment::get).oracle().username());

        environment.put("ORACLE_APP_USER", "app_user");
        assertEquals("app_user", AppConfig.from(environment::get).oracle().username());
    }
}
