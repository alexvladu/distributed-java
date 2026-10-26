package com.example.asset.control;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;

public final class UserControl {
    private final UserRepository users;
    private final PortfolioControl portfolios;

    public UserControl(UserRepository users, PortfolioControl portfolios) {
        this.users = users;
        this.portfolios = portfolios;
    }

    public User get(long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User update(long userId, long id, String username, String email) {
        if (userId != id) {
            throw new SecurityException("You can update only your own profile");
        }
        String validUsername = AccountRules.username(username);
        String validEmail = AccountRules.email(email);
        User current = get(userId);
        if (!current.username().equalsIgnoreCase(validUsername) && users.existsByUsername(validUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }
        return users.update(id, validUsername, validEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Removes the user's portfolios (SQL Server) and trades (Oracle) before the PostgreSQL identity. */
    public void delete(long userId, long id) {
        if (userId != id) {
            throw new SecurityException("You can delete only your own account");
        }
        for (Portfolio portfolio : portfolios.list(userId)) {
            portfolios.delete(userId, portfolio.portfolioId());
        }
        users.delete(id);
    }
}
