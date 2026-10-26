package com.example.asset.entity.trade;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.example.asset.entity.database.Jdbc;

/** Trades live in Oracle; {@code portfolio_id} is a logical reference to SQL Server portfolios. */
public final class JdbcTradeRepository implements TradeRepository {
    private static final String COLUMNS = "trade_id, portfolio_id, symbol, side, quantity, price, executed_at";

    private final DataSource dataSource;

    public JdbcTradeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Trade create(long portfolioId, String symbol, TradeSide side, BigDecimal quantity, BigDecimal price) {
        String sql = "INSERT INTO trades (portfolio_id, symbol, side, quantity, price) VALUES (?, ?, ?, ?, ?)";
        // Oracle returns a ROWID for RETURN_GENERATED_KEYS, so the identity column is requested by name.
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql, new String[] {"TRADE_ID"})) {
            statement.setLong(1, portfolioId);
            statement.setString(2, symbol);
            statement.setString(3, side.name());
            statement.setBigDecimal(4, quantity);
            statement.setBigDecimal(5, price);
            statement.executeUpdate();
            long tradeId = generatedTradeId(statement);
            return findById(tradeId).orElseThrow(
                    () -> new IllegalStateException("Trade " + tradeId + " was not found after insert"));
        } catch (SQLException exception) {
            throw Jdbc.failure("recording trade", exception);
        }
    }

    private static long generatedTradeId(PreparedStatement statement) throws SQLException {
        try (var keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Oracle did not return the generated trade_id");
            }
            return keys.getLong(1);
        }
    }

    @Override
    public Optional<Trade> findById(long id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM trades WHERE trade_id = ?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading trade", exception);
        }
    }

    @Override
    public List<Trade> findByPortfolioId(long portfolioId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM trades WHERE portfolio_id = ? ORDER BY executed_at DESC, trade_id DESC")) {
            statement.setLong(1, portfolioId);
            try (var result = statement.executeQuery()) {
                List<Trade> trades = new ArrayList<>();
                while (result.next()) {
                    trades.add(map(result));
                }
                return trades;
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading trades", exception);
        }
    }

    @Override
    public Optional<Trade> updateQuantity(long id, BigDecimal quantity) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("UPDATE trades SET quantity = ? WHERE trade_id = ?")) {
            statement.setBigDecimal(1, quantity);
            statement.setLong(2, id);
            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("updating trade", exception);
        }
        return findById(id);
    }

    @Override
    public void delete(long id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("DELETE FROM trades WHERE trade_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("deleting trade", exception);
        }
    }

    @Override
    public void deleteByPortfolioId(long portfolioId) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("DELETE FROM trades WHERE portfolio_id = ?")) {
            statement.setLong(1, portfolioId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("deleting trades", exception);
        }
    }

    private static Trade map(ResultSet result) throws SQLException {
        return new Trade(result.getLong("trade_id"), result.getLong("portfolio_id"), result.getString("symbol"),
                TradeSide.valueOf(result.getString("side")), result.getBigDecimal("quantity"),
                result.getBigDecimal("price"), result.getObject("executed_at", OffsetDateTime.class));
    }
}
