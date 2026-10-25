package com.example.asset.entity.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {
    }

    public static Connection postgres() throws SQLException {
        return connect("POSTGRES_URL", "jdbc:postgresql://localhost:5432/stock_market",
                "POSTGRES_USER", "stock_app", "POSTGRES_PASSWORD", "stock_app_password");
    }

    public static Connection sqlServer() throws SQLException {
        return connect("SQLSERVER_URL",
                "jdbc:sqlserver://localhost:1433;databaseName=stock_market;encrypt=false",
                "SQLSERVER_USER", "sa", "SQLSERVER_PASSWORD", "StockMarket_SA_123!");
    }

    public static Connection oracle() throws SQLException {
        return connect("ORACLE_URL", "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
                "ORACLE_USER", "stock_app", "ORACLE_APP_PASSWORD", "stock_app_password");
    }

    private static Connection connect(String urlName, String urlDefault, String userName,
            String userDefault, String passwordName, String passwordDefault) throws SQLException {
        return DriverManager.getConnection(env(urlName, urlDefault), env(userName, userDefault),
                env(passwordName, passwordDefault));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
