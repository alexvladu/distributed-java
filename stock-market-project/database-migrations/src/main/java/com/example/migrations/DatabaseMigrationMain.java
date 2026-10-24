package com.example.migrations;

import org.flywaydb.core.Flyway;

import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseMigrationMain {
    private DatabaseMigrationMain() {
    }

    public static void main(String[] args) {
        migrate("PostgreSQL", postgres());
        ensureSqlServerDatabase();
        migrate("SQL Server", sqlServer());
        migrate("OracleDB", oracle());
    }

    private static void migrate(String name, Flyway flyway) {
        System.out.println("Applying " + name + " migrations");
        flyway.migrate();
        System.out.println(name + " migrations completed");
    }

    private static Flyway postgres() {
        return Flyway.configure()
                .dataSource(env("POSTGRES_URL", "jdbc:postgresql://postgres:5432/" + env("POSTGRES_DB", "stock_market")),
                        env("POSTGRES_USER", "stock_app"), env("POSTGRES_PASSWORD", "stock_app_password"))
                .locations("classpath:db/migration/postgres").load();
    }

    private static Flyway sqlServer() {
        return Flyway.configure()
                .dataSource(env("SQLSERVER_URL",
                                "jdbc:sqlserver://sqlserver:1433;databaseName=stock_market;encrypt=false"),
                        "sa", env("MSSQL_SA_PASSWORD", "StockMarket_SA_123!"))
                .locations("classpath:db/migration/sqlserver").load();
    }

    private static void ensureSqlServerDatabase() {
        String url = env("SQLSERVER_ADMIN_URL",
                "jdbc:sqlserver://sqlserver:1433;databaseName=master;encrypt=false");
        try (var connection = DriverManager.getConnection(
                url, "sa", env("MSSQL_SA_PASSWORD", "StockMarket_SA_123!"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    IF DB_ID(N'stock_market') IS NULL
                        CREATE DATABASE stock_market
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create or verify SQL Server database stock_market", exception);
        }
    }

    private static Flyway oracle() {
        return Flyway.configure()
                .dataSource(env("ORACLE_URL", "jdbc:oracle:thin:@oracle:1521/FREEPDB1"),
                        env("ORACLE_APP_USER", "stock_app"), env("ORACLE_APP_PASSWORD", "stock_app_password"))
                .locations("classpath:db/migration/oracle").load();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
