package com.example.asset.entity.user;

import com.example.asset.entity.database.Database;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    public User create(String username, String email) {
        String sql = "INSERT INTO users (username, email) VALUES (?, ?) RETURNING user_id, username, email, created_at";
        try (var connection = Database.postgres(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            try (var result = statement.executeQuery()) {
                result.next();
                return map(result);
            }
        } catch (SQLException exception) {
            throw failure("creating user", exception);
        }
    }

    public Optional<User> findById(long id) {
        return queryOne("SELECT user_id, username, email, created_at FROM users WHERE user_id = ?", id);
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (var connection = Database.postgres();
                var statement = connection.prepareStatement(
                        "SELECT user_id, username, email, created_at FROM users ORDER BY user_id");
                var result = statement.executeQuery()) {
            while (result.next()) users.add(map(result));
            return users;
        } catch (SQLException exception) {
            throw failure("reading users", exception);
        }
    }

    public User update(long id, String username, String email) {
        try (var connection = Database.postgres();
                var statement = connection.prepareStatement(
                        "UPDATE users SET username = ?, email = ? WHERE user_id = ? RETURNING user_id, username, email, created_at")) {
            statement.setString(1, username); statement.setString(2, email); statement.setLong(3, id);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("User not found: " + id);
                return map(result);
            }
        } catch (SQLException exception) { throw failure("updating user", exception); }
    }

    public void delete(long id) {
        try (var connection = Database.postgres();
                var statement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            statement.setLong(1, id); statement.executeUpdate();
        } catch (SQLException exception) { throw failure("deleting user", exception); }
    }

    private Optional<User> queryOne(String sql, long id) {
        try (var connection = Database.postgres(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) { throw failure("reading user", exception); }
    }

    private User map(ResultSet result) throws SQLException {
        return new User(result.getLong("user_id"), result.getString("username"),
                result.getString("email"), result.getObject("created_at", java.time.OffsetDateTime.class));
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Database error while " + action, exception);
    }
}
