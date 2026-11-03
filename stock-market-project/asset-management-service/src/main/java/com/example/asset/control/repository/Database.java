package com.example.asset.control.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.example.asset.application.AppConfig;
import com.example.asset.application.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * Owns one connection pool per database engine; created at deployment so missing settings stop the deploy.
 * JTA would not enlist these pools, so every bean uses bean-managed (local JDBC) transactions.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class Database {
    private HikariDataSource postgres;
    private HikariDataSource sqlServer;
    private HikariDataSource oracle;

    @PostConstruct
    void start() {
        AppConfig config = AppConfig.fromEnvironment();
        postgres = pool(config.postgres());
        sqlServer = pool(config.sqlServer());
        oracle = pool(config.oracle());
    }

    public DataSource postgres() {
        return postgres;
    }

    public DataSource sqlServer() {
        return sqlServer;
    }

    public DataSource oracle() {
        return oracle;
    }

    /** Returns whether each database answers a validation check. */
    public Map<String, Boolean> ping() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("postgres", isReachable(postgres));
        result.put("sqlserver", isReachable(sqlServer));
        result.put("oracle", isReachable(oracle));
        return result;
    }

    @PreDestroy
    void close() {
        postgres.close();
        sqlServer.close();
        oracle.close();
    }

    private static boolean isReachable(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }

    private static HikariDataSource pool(DataSourceConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName(config.poolName());
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setDriverClassName(config.driverClassName());
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(5_000);
        // Start even if a database is still booting; connections are retried on demand.
        hikari.setInitializationFailTimeout(-1);
        return new HikariDataSource(hikari);
    }
}
