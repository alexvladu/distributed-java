package com.example.asset.control;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.user.User;
import com.example.asset.entity.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DashboardControl {
    public record Dashboard(User user, List<Portfolio> portfolios, Map<Long, List<Trade>> trades) {}
    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final TradeRepository trades;
    public DashboardControl(UserRepository users, PortfolioRepository portfolios, TradeRepository trades) {
        this.users = users; this.portfolios = portfolios; this.trades = trades;
    }
    public Dashboard load(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<Portfolio> owned = portfolios.findByUserId(userId);
        Map<Long, List<Trade>> byPortfolio = owned.stream().collect(Collectors.toMap(
                Portfolio::portfolioId, portfolio -> trades.findByPortfolioId(portfolio.portfolioId())));
        return new Dashboard(user, owned, byPortfolio);
    }
}
