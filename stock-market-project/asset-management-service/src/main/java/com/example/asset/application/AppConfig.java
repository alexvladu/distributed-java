package com.example.asset.application;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.example.asset.entity.database.DataSourceConfig;
import com.example.stock.contract.StockPriceEvent;

/** Runtime settings read from environment variables; secrets have no built-in defaults. */
public record AppConfig(DataSourceConfig postgres, DataSourceConfig sqlServer, DataSourceConfig oracle,
        String kafkaBootstrapServers, String kafkaTopic) {

    public static AppConfig fromEnvironment() {
        return from(System::getenv);
    }

    static AppConfig from(Function<String, String> environment) {
        Reader env = new Reader(environment);
        AppConfig config = new AppConfig(
                new DataSourceConfig("postgres",
                        env.optional("POSTGRES_URL", "jdbc:postgresql://localhost:5432/stock_market"),
                        env.optional("POSTGRES_USER", "stock_app"),
                        env.required("POSTGRES_PASSWORD"),
                        "org.postgresql.Driver"),
                new DataSourceConfig("sqlserver",
                        env.optional("SQLSERVER_URL",
                                "jdbc:sqlserver://localhost:1433;databaseName=stock_market;encrypt=false"),
                        env.optional("SQLSERVER_USER", "stock_app"),
                        env.required("SQLSERVER_PASSWORD"),
                        "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
                new DataSourceConfig("oracle",
                        env.optional("ORACLE_URL", "jdbc:oracle:thin:@localhost:1521/FREEPDB1"),
                        env.optional("ORACLE_APP_USER", env.optional("ORACLE_USER", "stock_app")),
                        env.required("ORACLE_APP_PASSWORD"),
                        "oracle.jdbc.OracleDriver"),
                env.optional("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
                env.optional("KAFKA_TOPIC", StockPriceEvent.DEFAULT_TOPIC));
        if (!env.missing.isEmpty()) {
            throw new IllegalStateException("Missing required environment variables: "
                    + String.join(", ", env.missing)
                    + ". Copy stock-market-project/.env.example to .env (run.sh does this automatically).");
        }
        return config;
    }

    private static final class Reader {
        private final Function<String, String> environment;
        private final List<String> missing = new ArrayList<>();

        private Reader(Function<String, String> environment) {
            this.environment = environment;
        }

        private String optional(String name, String fallback) {
            String value = environment.apply(name);
            return value == null || value.isBlank() ? fallback : value;
        }

        private String required(String name) {
            String value = environment.apply(name);
            if (value == null || value.isBlank()) {
                missing.add(name);
                return null;
            }
            return value;
        }
    }
}
