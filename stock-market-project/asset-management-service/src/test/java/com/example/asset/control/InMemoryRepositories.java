package com.example.asset.control;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.example.asset.entity.auth.UserCredentialRepository;
import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.trade.TradeSide;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;

/** In-memory repositories that mimic the JDBC implementations' contracts. */
final class InMemoryRepositories {
    private InMemoryRepositories() {
    }

    static final class Portfolios implements PortfolioRepository {
        final Map<Long, Portfolio> rows = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Portfolio create(long userId, String name, BigDecimal cashBalance) {
            Portfolio portfolio = new Portfolio(nextId++, userId, name, cashBalance, Instant.now());
            rows.put(portfolio.portfolioId(), portfolio);
            return portfolio;
        }

        @Override
        public Optional<Portfolio> findById(long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public List<Portfolio> findByUserId(long userId) {
            return rows.values().stream().filter(portfolio -> portfolio.userId() == userId).toList();
        }

        @Override
        public Optional<Portfolio> rename(long id, String name) {
            Portfolio current = rows.get(id);
            if (current == null) {
                return Optional.empty();
            }
            Portfolio renamed = new Portfolio(id, current.userId(), name, current.cashBalance(), current.createdAt());
            rows.put(id, renamed);
            return Optional.of(renamed);
        }

        @Override
        public void delete(long id) {
            rows.remove(id);
        }

        @Override
        public boolean adjustCash(long id, BigDecimal delta) {
            Portfolio current = rows.get(id);
            if (current == null) {
                return false;
            }
            BigDecimal next = current.cashBalance().add(delta);
            if (next.signum() < 0) {
                return false;
            }
            rows.put(id, new Portfolio(id, current.userId(), current.portfolioName(), next, current.createdAt()));
            return true;
        }

        BigDecimal cash(long id) {
            return rows.get(id).cashBalance();
        }
    }

    static final class Trades implements TradeRepository {
        final List<Trade> rows = new ArrayList<>();
        RuntimeException createFailure;
        RuntimeException deleteFailure;
        private long nextId = 1;

        @Override
        public Trade create(long portfolioId, String symbol, TradeSide side, BigDecimal quantity, BigDecimal price) {
            if (createFailure != null) {
                throw createFailure;
            }
            Trade trade = new Trade(nextId++, portfolioId, symbol, side, quantity, price,
                    OffsetDateTime.now(ZoneOffset.UTC));
            rows.add(trade);
            return trade;
        }

        @Override
        public Optional<Trade> findById(long id) {
            return rows.stream().filter(trade -> trade.tradeId() == id).findFirst();
        }

        @Override
        public List<Trade> findByPortfolioId(long portfolioId) {
            List<Trade> result = new ArrayList<>(
                    rows.stream().filter(trade -> trade.portfolioId() == portfolioId).toList());
            Collections.reverse(result);
            return result;
        }

        @Override
        public Optional<Trade> updateQuantity(long id, BigDecimal quantity) {
            for (int index = 0; index < rows.size(); index++) {
                Trade current = rows.get(index);
                if (current.tradeId() == id) {
                    Trade updated = new Trade(id, current.portfolioId(), current.symbol(), current.side(), quantity,
                            current.price(), current.executedAt());
                    rows.set(index, updated);
                    return Optional.of(updated);
                }
            }
            return Optional.empty();
        }

        @Override
        public void delete(long id) {
            rows.removeIf(trade -> trade.tradeId() == id);
        }

        @Override
        public void deleteByPortfolioId(long portfolioId) {
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            rows.removeIf(trade -> trade.portfolioId() == portfolioId);
        }
    }

    /** Users and credentials share one store, like the PostgreSQL tables they model. */
    static final class Accounts implements UserRepository, UserCredentialRepository {
        final Map<Long, User> users = new LinkedHashMap<>();
        final Map<Long, Credential> credentials = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public User createAccount(String username, String email, String passwordHash) {
            if (existsByUsername(username)) {
                throw new IllegalArgumentException("Username or email is already registered");
            }
            User user = new User(nextId++, username, email, OffsetDateTime.now(ZoneOffset.UTC));
            users.put(user.userId(), user);
            credentials.put(user.userId(), new Credential(user.userId(), username, passwordHash, 0, null));
            return user;
        }

        @Override
        public Optional<Credential> findByUsername(String username) {
            return credentials.values().stream()
                    .filter(credential -> credential.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public void recordFailedAttempt(long userId) {
            Credential current = credentials.get(userId);
            int attempts = current.failedAttempts() + 1;
            OffsetDateTime lockedUntil = attempts >= 5 ? OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15) : null;
            credentials.put(userId, new Credential(userId, current.username(), current.passwordHash(), attempts,
                    lockedUntil));
        }

        @Override
        public void recordSuccessfulLogin(long userId) {
            Credential current = credentials.get(userId);
            credentials.put(userId, new Credential(userId, current.username(), current.passwordHash(), 0, null));
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public boolean existsByUsername(String username) {
            String lower = username.toLowerCase(Locale.ROOT);
            return users.values().stream().anyMatch(user -> user.username().toLowerCase(Locale.ROOT).equals(lower));
        }

        @Override
        public Optional<User> update(long id, String username, String email) {
            User current = users.get(id);
            if (current == null) {
                return Optional.empty();
            }
            User updated = new User(id, username, email, current.createdAt());
            users.put(id, updated);
            return Optional.of(updated);
        }

        @Override
        public void delete(long id) {
            users.remove(id);
            credentials.remove(id);
        }
    }
}
