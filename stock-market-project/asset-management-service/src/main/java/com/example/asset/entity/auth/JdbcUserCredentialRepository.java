package com.example.asset.entity.auth;

import com.example.asset.entity.database.Database;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcUserCredentialRepository implements UserCredentialRepository {
    public void create(long userId, String passwordHash) {
        try (var c = Database.postgres();
                var s = c.prepareStatement("INSERT INTO user_credentials (user_id, password_hash) VALUES (?, ?)")) {
            s.setLong(1, userId);
            s.setString(2, passwordHash);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while creating credentials", e);
        }
    }

    public Optional<Credential> findByUsername(String username) {
        String sql = """
                SELECT u.user_id, u.username, c.password_hash, c.failed_attempts, c.locked_until
                FROM users u JOIN user_credentials c ON c.user_id = u.user_id
                WHERE u.username = ?
                """;
        try (var c = Database.postgres(); var s = c.prepareStatement(sql)) {
            s.setString(1, username);
            try (var r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new Credential(r.getLong("user_id"), r.getString("username"),
                        r.getString("password_hash"), r.getInt("failed_attempts"),
                        r.getObject("locked_until", java.time.OffsetDateTime.class)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while reading credentials", e);
        }
    }

    public void recordFailedAttempt(long userId, int failedAttempts) {
        try (var c = Database.postgres();
                var s = c.prepareStatement(
                        "UPDATE user_credentials SET failed_attempts = ?, locked_until = CASE WHEN ? >= 5 THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes' ELSE NULL END, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
            s.setInt(1, failedAttempts); s.setInt(2, failedAttempts); s.setLong(3, userId); s.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Database error while recording login failure", e); }
    }

    public void recordSuccessfulLogin(long userId) {
        try (var c = Database.postgres();
                var s = c.prepareStatement(
                        "UPDATE user_credentials SET failed_attempts = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
            s.setLong(1, userId); s.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Database error while recording login", e); }
    }
}
