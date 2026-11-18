package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.asset.control.InMemoryRepositories.Accounts;
import com.example.asset.control.InMemoryRepositories.Portfolios;
import com.example.asset.control.InMemoryRepositories.Trades;
import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.trade.TradeSide;
import com.example.asset.entity.user.User;

class PortfolioControlTest {
    private static final long ALICE = 1;
    private static final long BOB = 2;

    private final Portfolios portfolios = new Portfolios();
    private final Trades trades = new Trades();
    private final PortfolioControl control = new PortfolioControl(portfolios, trades);

    @Test
    void createsTrimmedPortfoliosWithInitialCash() {
        Portfolio portfolio = control.create(ALICE, "  Long term  ");

        assertEquals("Long term", portfolio.portfolioName());
        assertEquals(0, PortfolioControl.INITIAL_CASH.compareTo(portfolio.cashBalance()));
    }

    @Test
    void validatesNamesAndLimitsThePortfolioCount() {
        assertThrows(IllegalArgumentException.class, () -> control.create(ALICE, " "));
        assertThrows(IllegalArgumentException.class, () -> control.create(ALICE, null));
        assertThrows(IllegalArgumentException.class, () -> control.create(ALICE, "x".repeat(121)));
        for (int index = 0; index < PortfolioControl.MAX_PORTFOLIOS_PER_USER; index++) {
            control.create(ALICE, "Portfolio " + index);
        }
        assertThrows(IllegalArgumentException.class, () -> control.create(ALICE, "One too many"));
    }

    @Test
    void otherUsersCannotReadRenameOrDeleteAPortfolio() {
        long id = control.create(ALICE, "Private").portfolioId();

        assertThrows(NotFoundException.class, () -> control.get(BOB, id));
        assertThrows(NotFoundException.class, () -> control.rename(BOB, id, "Mine now"));
        assertThrows(NotFoundException.class, () -> control.delete(BOB, id));
        assertEquals("Private", control.get(ALICE, id).portfolioName());
        assertThrows(NotFoundException.class, () -> control.get(ALICE, 999));
    }

    @Test
    void deletingAPortfolioRemovesItsTrades() {
        long kept = control.create(ALICE, "Kept").portfolioId();
        long removed = control.create(ALICE, "Removed").portfolioId();
        trades.create(kept, "AAPL", TradeSide.BUY, BigDecimal.ONE, BigDecimal.TEN);
        trades.create(removed, "AAPL", TradeSide.BUY, BigDecimal.ONE, BigDecimal.TEN);

        control.delete(ALICE, removed);

        assertTrue(portfolios.findById(removed).isEmpty());
        assertEquals(1, trades.rows.size());
        assertEquals(kept, trades.rows.get(0).portfolioId());
    }

    @Test
    void portfolioDeletionSucceedsEvenIfTradeCleanupFails() {
        long id = control.create(ALICE, "Temporary").portfolioId();
        trades.deleteFailure = new IllegalStateException("Oracle is unavailable");

        control.delete(ALICE, id);

        assertTrue(portfolios.findById(id).isEmpty());
    }

    @Test
    void deletingAUserRemovesTheirPortfolios() {
        Accounts accounts = new Accounts();
        User user = accounts.createAccount("alice", "alice@example.com", "hash");
        control.create(user.userId(), "One");
        control.create(user.userId(), "Two");
        UserControl users = new UserControl(accounts, control);

        assertThrows(SecurityException.class, () -> users.delete(user.userId(), user.userId() + 1));
        users.delete(user.userId(), user.userId());

        assertTrue(accounts.findById(user.userId()).isEmpty());
        assertTrue(portfolios.findByUserId(user.userId()).isEmpty());
    }

    @Test
    void profileUpdatesKeepUsernamesUnique() {
        Accounts accounts = new Accounts();
        User alice = accounts.createAccount("alice", "alice@example.com", "hash");
        accounts.createAccount("bob", "bob@example.com", "hash");
        UserControl users = new UserControl(accounts, control);

        assertThrows(IllegalArgumentException.class,
                () -> users.update(alice.userId(), alice.userId(), "BOB", "alice@example.com"));
        assertEquals("Alice", users.update(alice.userId(), alice.userId(), "Alice", "new@example.com").username());
    }
}
