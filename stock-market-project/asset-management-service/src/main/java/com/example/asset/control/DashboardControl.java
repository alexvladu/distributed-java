package com.example.asset.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.asset.control.repository.TradeRepository;
import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.user.User;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/** Joins the three databases in memory: user (PostgreSQL), portfolios (SQL Server), trades (Oracle). */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class DashboardControl {
    public record Dashboard(User user, List<Portfolio> portfolios, Map<Long, List<Trade>> trades) {
    }

    @EJB
    private UserControl users;
    @EJB
    private PortfolioControl portfolios;
    @EJB
    private TradeRepository trades;

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
