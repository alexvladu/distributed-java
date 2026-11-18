package com.example.asset.control.repository;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.asset.application.AppConfig;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Health checks for the three persistence units. Created at deployment so a missing database password stops
 * the deploy; it has no transaction, so each check runs on its own connection.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class Database {
    @PersistenceContext(unitName = "postgres")
    private EntityManager postgres;
    @PersistenceContext(unitName = "sqlserver")
    private EntityManager sqlServer;
    @PersistenceContext(unitName = "oracle")
    private EntityManager oracle;

    @PostConstruct
    void start() {
        AppConfig.requireDatabasePasswords();
    }

    /** Returns whether each database answers a trivial query. */
    public Map<String, Boolean> ping() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("postgres", isReachable(postgres, "SELECT 1"));
        result.put("sqlserver", isReachable(sqlServer, "SELECT 1"));
        result.put("oracle", isReachable(oracle, "SELECT 1 FROM DUAL"));
        return result;
    }

    private static boolean isReachable(EntityManager entityManager, String query) {
        try {
            entityManager.createNativeQuery(query).getSingleResult();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
