package com.example.asset.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.user.User;

/** Joins the three databases in memory: user (PostgreSQL), portfolios (SQL Server), trades (Oracle). */
public final class DashboardControl {
    public record Dashboard(User user, List<Portfolio> portfolios, Map<Long, List<Trade>> trades) {
    }

    private final UserControl users;
    private final PortfolioControl portfolios;
    private final TradeRepository trades;

    public DashboardControl(UserControl users, PortfolioControl portfolios, TradeRepository trades) {
        this.users = users;
        this.portfolios = portfolios;
        this.trades = trades;
    }

    public Dashboard load(long userId) {
        User user = users.get(userId);
        List<Portfolio> owned = portfolios.list(userId);
        Map<Long, List<Trade>> byPortfolio = new LinkedHashMap<>();
        for (Portfolio portfolio : owned) {
            byPortfolio.put(portfolio.portfolioId(), trades.findByPortfolioId(portfolio.portfolioId()));
        }
        return new Dashboard(user, owned, byPortfolio);
    }
}
