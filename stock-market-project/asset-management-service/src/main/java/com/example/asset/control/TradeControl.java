package com.example.asset.control;

import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;

public final class TradeControl {
    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final TradeRepository trades;
    public TradeControl(UserRepository users, PortfolioRepository portfolios, TradeRepository trades) {
        this.users = users; this.portfolios = portfolios; this.trades = trades;
    }
    public List<Trade> findByPortfolio(long userId, long portfolioId) {
        verifyPortfolio(userId, portfolioId);
        return trades.findByPortfolioId(portfolioId);
    }
    public Trade find(long id) { return trades.findById(id).orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id)); }
    public Trade create(long userId, long portfolioId, String symbol, String side, BigDecimal quantity) {
        verifyPortfolio(userId, portfolioId); validate(symbol, side, quantity);
        return trades.create(portfolioId, symbol.trim().toUpperCase(), side.trim().toUpperCase(), quantity);
    }
    public Trade update(long userId, long id, String symbol, String side, BigDecimal quantity) {
        Trade existing = find(id);
        verifyPortfolio(userId, existing.portfolioId());
        validate(symbol, side, quantity);
        return trades.update(id, symbol.trim().toUpperCase(), side.trim().toUpperCase(), quantity);
    }
    public void delete(long userId, long id) {
        Trade existing = find(id);
        verifyPortfolio(userId, existing.portfolioId());
        trades.delete(id);
    }
    private void verifyPortfolio(long userId, long portfolioId) {
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        PortfolioRepository p = portfolios;
        var portfolio = p.findById(portfolioId).orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        if (portfolio.userId() != userId) throw new SecurityException("Portfolio does not belong to user");
    }
    private void validate(String symbol, String side, BigDecimal quantity) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (!"BUY".equalsIgnoreCase(side) && !"SELL".equalsIgnoreCase(side)) throw new IllegalArgumentException("side must be BUY or SELL");
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("quantity must be positive");
    }
}
