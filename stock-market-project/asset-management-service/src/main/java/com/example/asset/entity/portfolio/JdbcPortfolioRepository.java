package com.example.asset.entity.portfolio;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.example.asset.entity.database.Jdbc;

/** Portfolios live in SQL Server; {@code user_id} is a logical reference to PostgreSQL users. */
public final class JdbcPortfolioRepository implements PortfolioRepository {
    private static final String TABLE = "stock_market.dbo.portfolios";
    private static final String COLUMNS = "portfolio_id, user_id, portfolio_name, cash_balance, created_at";
    private static final String INSERTED = "INSERTED.portfolio_id, INSERTED.user_id, INSERTED.portfolio_name, "
            + "INSERTED.cash_balance, INSERTED.created_at";
    private static final String BY_ID = " WHERE portfolio_id = ?";

    private final DataSource dataSource;

    public JdbcPortfolioRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Portfolio create(long userId, String name, BigDecimal cashBalance) {
        String sql = "INSERT INTO " + TABLE + " (user_id, portfolio_name, cash_balance) OUTPUT " + INSERTED
                + " VALUES (?, ?, ?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setBigDecimal(3, cashBalance);
            try (var result = statement.executeQuery()) {
                result.next();
                return map(result);
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("creating portfolio", exception);
        }
    }

    @Override
    public Optional<Portfolio> findById(long id) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM " + TABLE + BY_ID)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading portfolio", exception);
        }
    }

    @Override
    public List<Portfolio> findByUserId(long userId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE user_id = ? ORDER BY portfolio_id")) {
            statement.setLong(1, userId);
            try (var result = statement.executeQuery()) {
                List<Portfolio> portfolios = new ArrayList<>();
                while (result.next()) {
                    portfolios.add(map(result));
                }
                return portfolios;
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("reading portfolios", exception);
        }
    }

    @Override
    public Optional<Portfolio> rename(long id, String name) {
        String sql = "UPDATE " + TABLE + " SET portfolio_name = ? OUTPUT " + INSERTED + BY_ID;
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw Jdbc.failure("renaming portfolio", exception);
        }
    }

    @Override
    public void delete(long id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("DELETE FROM " + TABLE + BY_ID)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw Jdbc.failure("deleting portfolio", exception);
        }
    }

    @Override
    public boolean adjustCash(long id, BigDecimal delta) {
        String sql = "UPDATE " + TABLE + " SET cash_balance = cash_balance + ? "
                + "WHERE portfolio_id = ? AND cash_balance + ? >= 0";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, delta);
            statement.setLong(2, id);
            statement.setBigDecimal(3, delta);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw Jdbc.failure("updating cash balance", exception);
        }
    }

    private static Portfolio map(ResultSet result) throws SQLException {
        return new Portfolio(result.getLong("portfolio_id"), result.getLong("user_id"),
                result.getString("portfolio_name"), result.getBigDecimal("cash_balance"),
                result.getTimestamp("created_at").toLocalDateTime().toInstant(ZoneOffset.UTC));
    }
}
