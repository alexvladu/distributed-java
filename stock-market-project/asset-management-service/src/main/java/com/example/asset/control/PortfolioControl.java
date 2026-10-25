package com.example.asset.control;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.user.UserRepository;
import java.util.List;

public final class PortfolioControl {
    private final UserRepository users;
    private final PortfolioRepository portfolios;
    public PortfolioControl(UserRepository users, PortfolioRepository portfolios) {
        this.users = users; this.portfolios = portfolios;
    }
    public List<Portfolio> findByUser(long userId) {
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return portfolios.findByUserId(userId);
    }
    public Portfolio find(long id) { return portfolios.findById(id).orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id)); }
    public Portfolio create(long userId, String name) {
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        require(name);
        return portfolios.create(userId, name.trim());
    }
    public Portfolio update(long userId, long id, String name) {
        verifyOwner(userId, id); require(name); return portfolios.update(id, name.trim());
    }
    public void delete(long userId, long id) { verifyOwner(userId, id); portfolios.delete(id); }
    private void verifyOwner(long userId, long id) {
        var portfolio = portfolios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id));
        if (portfolio.userId() != userId) throw new SecurityException("Portfolio does not belong to user");
    }
    private void require(String name) { if (name == null || name.isBlank()) throw new IllegalArgumentException("portfolioName is required"); }
}
