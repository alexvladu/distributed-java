package com.example.asset.entity.auth;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.sql.DataSource;

import com.example.asset.entity.database.Jdbc;
import com.example.asset.entity.user.JdbcUserRepository;
import com.example.asset.entity.user.User;

/** Credentials live in PostgreSQL next to the users table. */
public final class JdbcUserCredentialRepository implements UserCredentialRepository {
    private final DataSource dataSource;

    public JdbcUserCredentialRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User createAccount(String username, String email, String passwordHash) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                User user;
                try (var statement = connection.prepareStatement(
                        "INSERT INTO users (username, email) VALUES (?, ?) "
                                + "RETURNING user_id, username, email, created_at")) {
                    statement.setString(1, username);
                    statement.setString(2, email);
                    try (var result = statement.executeQuery()) {
                        result.next();
                        user = JdbcUserRepository.map(result);
                    }
                }
                try (var statement = connection.prepareStatement(
                        "INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)")) {
                    statement.setLong(1, user.userId());
                    statement.setString(2, passwordHash);
                    statement.executeUpdate();
                }
                connection.commit();
                return user;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            if (Jdbc.isUniqueViolation(exception)) {
                throw new IllegalArgumentException("Username or email is already registered");
            }
            throw Jdbc.failure("creating account", exception);
        }
    }

    @Override
    public Optional<Credential> findByUsername(String username) {
        String sql = """
                SELECT u.user_id, u.username, c.password_hash, c.failed_attempts, c.locked_until
                FROM users u JOIN user_credentials c ON c.user_id = u.user_id
                WHERE LOWER(u.username) = LOWER(?)
                ORDER BY u.user_id
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Credential(result.getLong("user_id"), result.getString("username"),
                        result.getString("password_hash"), result.getInt("failed_attempts"),
                        result.getObject("locked_until", OffsetDateTime.class)));
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading credentials", exception);
        }
    }

    @Override
    public void recordFailedAttempt(long userId) {
        // SET expressions see the old row, so the lock triggers on the 5th consecutive failure.
        String sql = """
                UPDATE user_credentials
                SET failed_attempts = failed_attempts + 1,
                    locked_until = CASE WHEN failed_attempts + 1 >= 5
                        THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes' ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("recording login failure", exception);
        }
    }

    @Override
    public void recordSuccessfulLogin(long userId) {
        String sql = """
                UPDATE user_credentials
                SET failed_attempts = 0, locked_until = NULL,
                    last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("recording login", exception);
        }
    }
}
