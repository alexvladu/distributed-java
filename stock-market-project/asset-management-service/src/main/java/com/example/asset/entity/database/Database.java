package com.example.asset.entity.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Owns one connection pool per database engine. */
public final class Database implements AutoCloseable {
    private final HikariDataSource postgres;
    private final HikariDataSource sqlServer;
    private final HikariDataSource oracle;

    public Database(DataSourceConfig postgres, DataSourceConfig sqlServer, DataSourceConfig oracle) {
        this.postgres = pool(postgres);
        this.sqlServer = pool(sqlServer);
        this.oracle = pool(oracle);
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

    @Override
    public void close() {
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
