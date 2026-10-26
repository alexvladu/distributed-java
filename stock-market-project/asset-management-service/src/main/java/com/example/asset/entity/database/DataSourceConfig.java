package com.example.asset.entity.database;

/** Connection settings for one pooled database. */
public record DataSourceConfig(String poolName, String url, String username, String password,
        String driverClassName) {
}
