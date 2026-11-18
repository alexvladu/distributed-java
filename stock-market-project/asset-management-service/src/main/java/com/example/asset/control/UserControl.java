package com.example.asset.control;

import com.example.asset.control.repository.UserRepository;
import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.user.User;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class UserControl {
    @EJB
    private UserRepository users;
    @EJB
    private PortfolioControl portfolios;

    public UserControl() {
    }

    UserControl(UserRepository users, PortfolioControl portfolios) {
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
