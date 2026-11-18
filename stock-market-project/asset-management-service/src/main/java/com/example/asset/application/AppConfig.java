package com.example.asset.application;

import java.util.List;
import java.util.function.Function;

/**
 * Checks the environment variables that have no default. The data sources in WEB-INF/resources.xml read the
 * database variables; checking the passwords here stops the deployment with a clear message.
 */
public final class AppConfig {
    private static final List<String> DATABASE_PASSWORDS =
            List.of("POSTGRES_PASSWORD", "SQLSERVER_PASSWORD", "ORACLE_APP_PASSWORD");

    private AppConfig() {
    }

    public static void requireDatabasePasswords() {
        requireDatabasePasswords(System::getenv);
    }

    static void requireDatabasePasswords(Function<String, String> environment) {
        List<String> missing = DATABASE_PASSWORDS.stream()
                .filter(name -> {
                    String value = environment.apply(name);
                    return value == null || value.isBlank();
                })
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: " + String.join(", ", missing)
                    + ". Copy stock-market-project/.env.example to .env (run.sh does this automatically).");
        }
    }
}
