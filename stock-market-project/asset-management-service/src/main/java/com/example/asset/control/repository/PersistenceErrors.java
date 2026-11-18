package com.example.asset.control.repository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Recognizes database errors that the repositories report as validation failures. */
final class PersistenceErrors {
    /** SQLSTATE of a unique-constraint violation. */
    private static final String UNIQUE_VIOLATION = "23505";

    private PersistenceErrors() {
    }

    /** The JPA provider keeps the driver's {@link SQLException} in the cause chain. */
    static boolean isUniqueViolation(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
