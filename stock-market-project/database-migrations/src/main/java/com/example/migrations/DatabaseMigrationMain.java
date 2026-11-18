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
        ensureSqlServerApplicationLogin();
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
                .dataSource(sqlServerUrl(), "sa", saPassword())
                .locations("classpath:db/migration/sqlserver").load();
    }

    /** Creates the least-privilege login the asset service uses instead of sa. */
    private static void ensureSqlServerApplicationLogin() {
        String login = env("SQLSERVER_USER", "stock_app");
        if ("sa".equalsIgnoreCase(login)) {
            System.out.println("SQLSERVER_USER is sa; skipping application login provisioning");
            return;
        }
        // DDL cannot be parameterized, so only plain identifiers are accepted and quotes are escaped.
        if (!login.matches("[A-Za-z_][A-Za-z0-9_]{0,63}")) {
            throw new IllegalStateException("SQLSERVER_USER must contain only letters, digits, and underscores");
        }
        String password = env("SQLSERVER_PASSWORD", "StockMarket_App_123!").replace("'", "''");
        try (var connection = DriverManager.getConnection(sqlServerAdminUrl(), "sa", saPassword());
             var statement = connection.createStatement()) {
            statement.execute("IF SUSER_ID(N'" + login + "') IS NULL "
                    + "CREATE LOGIN [" + login + "] WITH PASSWORD = N'" + password + "' "
                    + "ELSE IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = N'" + login + "' "
                    + "AND PWDCOMPARE(N'" + password + "', password_hash) = 1) "
                    + "ALTER LOGIN [" + login + "] WITH PASSWORD = N'" + password + "'");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create or update SQL Server login " + login, exception);
        }
        try (var connection = DriverManager.getConnection(sqlServerUrl(), "sa", saPassword());
             var statement = connection.createStatement()) {
            statement.execute("IF USER_ID(N'" + login + "') IS NULL CREATE USER [" + login + "] FOR LOGIN [" + login + "]");
            for (String role : new String[] {"db_datareader", "db_datawriter"}) {
                statement.execute("IF ISNULL(IS_ROLEMEMBER(N'" + role + "', N'" + login + "'), 0) = 0 "
                        + "ALTER ROLE " + role + " ADD MEMBER [" + login + "]");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to grant SQL Server permissions to " + login, exception);
        }
        System.out.println("SQL Server application login " + login + " is ready");
    }

    private static String sqlServerUrl() {
        return env("SQLSERVER_URL", "jdbc:sqlserver://sqlserver:1433;databaseName=stock_market;encrypt=false");
    }

    private static String sqlServerAdminUrl() {
        return env("SQLSERVER_ADMIN_URL", "jdbc:sqlserver://sqlserver:1433;databaseName=master;encrypt=false");
    }

    private static String saPassword() {
        return env("MSSQL_SA_PASSWORD", "StockMarket_SA_123!");
    }

    private static void ensureSqlServerDatabase() {
        try (var connection = DriverManager.getConnection(sqlServerAdminUrl(), "sa", saPassword());
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
