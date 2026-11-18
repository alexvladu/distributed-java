package com.example.asset.control.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

class PersistenceErrorsTest {
    @Test
    void findsTheUniqueViolationBehindTheProviderException() {
        SQLException driverError = new SQLException("duplicate key value", "23505");
        PersistenceException error = new PersistenceException("flush failed",
                new IllegalStateException("store error", driverError));

        assertTrue(PersistenceErrors.isUniqueViolation(error));
    }

    @Test
    void ignoresOtherDatabaseErrors() {
        assertFalse(PersistenceErrors.isUniqueViolation(
                new PersistenceException(new SQLException("canceling statement", "57014"))));
        assertFalse(PersistenceErrors.isUniqueViolation(new PersistenceException("no cause")));
    }
}
