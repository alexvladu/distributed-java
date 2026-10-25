package com.example.asset.entity.portfolio;

import com.example.asset.entity.database.Database;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcPortfolioRepository implements PortfolioRepository {
    private static final String COLUMNS = "portfolio_id, user_id, portfolio_name, created_at";
    public Portfolio create(long userId, String name) {
        String sql = "INSERT INTO stock_market.dbo.portfolios (user_id, portfolio_name) VALUES (?, ?)";
        try (var c = Database.sqlServer(); var s = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, userId); s.setString(2, name); s.executeUpdate();
            try (var keys = s.getGeneratedKeys()) { keys.next(); return findById(keys.getLong(1)).orElseThrow(); }
        } catch (SQLException e) { throw failure("creating portfolio", e); }
    }
    public Optional<Portfolio> findById(long id) {
        try (var c = Database.sqlServer(); var s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM stock_market.dbo.portfolios WHERE portfolio_id = ?")) {
            s.setLong(1, id); try (var r = s.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { throw failure("reading portfolio", e); }
    }
    public List<Portfolio> findByUserId(long userId) {
        List<Portfolio> result = new ArrayList<>();
        try (var c = Database.sqlServer(); var s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM stock_market.dbo.portfolios WHERE user_id = ? ORDER BY portfolio_id")) {
            s.setLong(1, userId); try (var r = s.executeQuery()) { while (r.next()) result.add(map(r)); return result; }
        } catch (SQLException e) { throw failure("reading portfolios", e); }
    }
    public Portfolio update(long id, String name) {
        try (var c = Database.sqlServer(); var s = c.prepareStatement(
                "UPDATE stock_market.dbo.portfolios SET portfolio_name = ? WHERE portfolio_id = ?")) {
            s.setString(1, name); s.setLong(2, id); if (s.executeUpdate() == 0) throw new IllegalArgumentException("Portfolio not found: " + id);
            return findById(id).orElseThrow();
        } catch (SQLException e) { throw failure("updating portfolio", e); }
    }
    public void delete(long id) {
        try (var c = Database.sqlServer(); var s = c.prepareStatement(
                "DELETE FROM stock_market.dbo.portfolios WHERE portfolio_id = ?")) {
            s.setLong(1, id); s.executeUpdate();
        } catch (SQLException e) { throw failure("deleting portfolio", e); }
    }
    private Portfolio map(java.sql.ResultSet r) throws SQLException {
        return new Portfolio(r.getLong("portfolio_id"), r.getLong("user_id"), r.getString("portfolio_name"),
                r.getTimestamp("created_at").toLocalDateTime());
    }
    private IllegalStateException failure(String action, SQLException e) { return new IllegalStateException("Database error while " + action, e); }
}
