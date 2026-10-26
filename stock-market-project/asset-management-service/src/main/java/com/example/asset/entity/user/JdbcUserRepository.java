package com.example.asset.entity.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.sql.DataSource;

import com.example.asset.entity.database.Jdbc;

/** Users live in PostgreSQL. */
public final class JdbcUserRepository implements UserRepository {
    private static final String COLUMNS = "user_id, username, email, created_at";

    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findById(long id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE user_id = ?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading user", exception);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?)")) {
            statement.setString(1, username);
            try (var result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("checking username", exception);
        }
    }

    @Override
    public Optional<User> update(long id, String username, String email) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "UPDATE users SET username = ?, email = ? WHERE user_id = ? RETURNING " + COLUMNS)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setLong(3, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            if (Jdbc.isUniqueViolation(exception)) {
                throw new IllegalArgumentException("Username or email is already in use");
            }
            throw Jdbc.failure("updating user", exception);
        }
    }

    @Override
    public void delete(long id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("deleting user", exception);
        }
    }

    public static User map(ResultSet result) throws SQLException {
        return new User(result.getLong("user_id"), result.getString("username"),
                result.getString("email"), result.getObject("created_at", OffsetDateTime.class));
    }
}
