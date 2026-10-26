package com.example.asset.entity.database;

import java.sql.SQLException;

public final class Jdbc {
    private static final String UNIQUE_VIOLATION = "23505";

    private Jdbc() {
    }

    public static IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Database error while " + action, exception);
    }

    public static boolean isUniqueViolation(SQLException exception) {
        return UNIQUE_VIOLATION.equals(exception.getSQLState());
    }
}
