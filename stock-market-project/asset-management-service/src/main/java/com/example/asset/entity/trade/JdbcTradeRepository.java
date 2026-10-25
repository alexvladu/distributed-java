package com.example.asset.entity.trade;

import com.example.asset.entity.database.Database;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcTradeRepository implements TradeRepository {
    private static final String COLUMNS = "trade_id, portfolio_id, symbol, side, quantity, executed_at";
    public Trade create(long portfolioId, String symbol, String side, BigDecimal quantity) {
        try (var c = Database.oracle(); var s = c.prepareStatement(
                "INSERT INTO trades (portfolio_id, symbol, side, quantity) VALUES (?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, portfolioId); s.setString(2, symbol); s.setString(3, side); s.setBigDecimal(4, quantity); s.executeUpdate();
            try (var keys = s.getGeneratedKeys()) { keys.next(); return findById(keys.getLong(1)).orElseThrow(); }
        } catch (SQLException e) { throw failure("creating trade", e); }
    }
    public Optional<Trade> findById(long id) { return query("SELECT " + COLUMNS + " FROM trades WHERE trade_id = ?", id); }
    public List<Trade> findByPortfolioId(long portfolioId) {
        List<Trade> result = new ArrayList<>();
        try (var c = Database.oracle(); var s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM trades WHERE portfolio_id = ? ORDER BY executed_at DESC")) {
            s.setLong(1, portfolioId); try (var r = s.executeQuery()) { while (r.next()) result.add(map(r)); return result; }
        } catch (SQLException e) { throw failure("reading trades", e); }
    }
    public Trade update(long id, String symbol, String side, BigDecimal quantity) {
        try (var c = Database.oracle(); var s = c.prepareStatement(
                "UPDATE trades SET symbol = ?, side = ?, quantity = ? WHERE trade_id = ?")) {
            s.setString(1, symbol); s.setString(2, side); s.setBigDecimal(3, quantity); s.setLong(4, id);
            if (s.executeUpdate() == 0) throw new IllegalArgumentException("Trade not found: " + id);
            return findById(id).orElseThrow();
        } catch (SQLException e) { throw failure("updating trade", e); }
    }
    public void delete(long id) {
        try (var c = Database.oracle(); var s = c.prepareStatement("DELETE FROM trades WHERE trade_id = ?")) {
            s.setLong(1, id); s.executeUpdate();
        } catch (SQLException e) { throw failure("deleting trade", e); }
    }
    private Optional<Trade> query(String sql, long id) {
        try (var c = Database.oracle(); var s = c.prepareStatement(sql)) {
            s.setLong(1, id); try (var r = s.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { throw failure("reading trade", e); }
    }
    private Trade map(java.sql.ResultSet r) throws SQLException {
        return new Trade(r.getLong("trade_id"), r.getLong("portfolio_id"), r.getString("symbol"), r.getString("side"),
                r.getBigDecimal("quantity"), r.getObject("executed_at", java.time.OffsetDateTime.class));
    }
    private IllegalStateException failure(String action, SQLException e) { return new IllegalStateException("Database error while " + action, e); }
}
