package com.example.asset.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void failsFastListingEveryMissingPassword() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> AppConfig.requireDatabasePasswords(Map.of("SQLSERVER_PASSWORD", " ")::get));

        assertTrue(error.getMessage().contains("POSTGRES_PASSWORD"), error.getMessage());
        assertTrue(error.getMessage().contains("SQLSERVER_PASSWORD"), error.getMessage());
        assertTrue(error.getMessage().contains("ORACLE_APP_PASSWORD"), error.getMessage());
    }

    @Test
    void acceptsAnEnvironmentWithEveryPassword() {
        Map<String, String> environment = Map.of(
                "POSTGRES_PASSWORD", "pg-secret",
                "SQLSERVER_PASSWORD", "mssql-secret",
                "ORACLE_APP_PASSWORD", "oracle-secret");

        assertDoesNotThrow(() -> AppConfig.requireDatabasePasswords(environment::get));
    }
}
