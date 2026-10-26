package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.asset.control.InMemoryRepositories.Accounts;
import com.example.asset.control.InMemoryRepositories.Portfolios;
import com.example.asset.control.InMemoryRepositories.Trades;
import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.user.User;

class AuthControlTest {
    private static final String PASSWORD = "correct horse battery";

    private final Accounts accounts = new Accounts();
    private final Portfolios portfolios = new Portfolios();
    private final AuthControl auth = new AuthControl(accounts, accounts,
            new PortfolioControl(portfolios, new Trades()));

    @Test
    void registrationStoresAHashAndCreatesAFundedDefaultPortfolio() {
        User user = auth.register(" alice ", "alice@example.com", PASSWORD);

        assertEquals("alice", user.username());
        String hash = accounts.credentials.get(user.userId()).passwordHash();
        assertTrue(hash.startsWith("pbkdf2$"), hash);
        assertFalse(hash.contains(PASSWORD));
        List<Portfolio> owned = portfolios.findByUserId(user.userId());
        assertEquals(1, owned.size());
        assertEquals(PortfolioControl.DEFAULT_NAME, owned.get(0).portfolioName());
        assertEquals(0, new BigDecimal("1000.00").compareTo(owned.get(0).cashBalance()));
    }

    @Test
    void registrationValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("ab", "a@example.com", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> auth.register("bad name", "a@example.com", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> auth.register("alice", "not-an-email", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> auth.register("alice", "a@example.com", "short"));
        assertThrows(IllegalArgumentException.class, () -> auth.register("alice", "a@example.com", null));
        assertTrue(accounts.users.isEmpty());
    }

    @Test
    void usernamesAreUniqueIgnoringCase() {
        auth.register("alice", "alice@example.com", PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> auth.register("ALICE", "other@example.com", PASSWORD));
    }

    @Test
    void authenticatesWithTheCorrectPasswordAndResetsFailures() {
        User registered = auth.register("alice", "alice@example.com", PASSWORD);
        assertThrows(SecurityException.class, () -> auth.authenticate("alice", "wrong password"));
        assertEquals(1, accounts.credentials.get(registered.userId()).failedAttempts());

        User authenticated = auth.authenticate("Alice", PASSWORD);

        assertEquals(registered.userId(), authenticated.userId());
        assertEquals(0, accounts.credentials.get(registered.userId()).failedAttempts());
    }

    @Test
    void locksTheAccountAfterRepeatedFailures() {
        User registered = auth.register("alice", "alice@example.com", PASSWORD);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(SecurityException.class, () -> auth.authenticate("alice", "wrong password"));
        }
        assertNotNull(accounts.credentials.get(registered.userId()).lockedUntil());

        SecurityException error = assertThrows(SecurityException.class, () -> auth.authenticate("alice", PASSWORD));
        assertTrue(error.getMessage().contains("locked"), error.getMessage());
    }

    @Test
    void unknownUsersAndBlankInputAreRejected() {
        assertThrows(SecurityException.class, () -> auth.authenticate("nobody", PASSWORD));
        assertThrows(SecurityException.class, () -> auth.authenticate(" ", PASSWORD));
        assertThrows(SecurityException.class, () -> auth.authenticate("alice", null));
    }
}
