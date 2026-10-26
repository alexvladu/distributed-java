package com.example.asset.control;

import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.util.List;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.TradeRepository;

public final class PortfolioControl {
    public static final BigDecimal INITIAL_CASH = new BigDecimal("1000.00");
    public static final String DEFAULT_NAME = "Main portfolio";
    static final int MAX_PORTFOLIOS_PER_USER = 20;
    private static final int MAX_NAME_LENGTH = 120;
    private static final System.Logger LOG = System.getLogger(PortfolioControl.class.getName());

    private final PortfolioRepository portfolios;
    private final TradeRepository trades;

    public PortfolioControl(PortfolioRepository portfolios, TradeRepository trades) {
        this.portfolios = portfolios;
        this.trades = trades;
    }

    public List<Portfolio> list(long userId) {
        return portfolios.findByUserId(userId);
    }

    /** Portfolios owned by someone else are reported as missing so their IDs cannot be probed. */
    public Portfolio get(long userId, long portfolioId) {
        return portfolios.findById(portfolioId)
                .filter(portfolio -> portfolio.userId() == userId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));
    }

    public Portfolio create(long userId, String name) {
        String validName = validateName(name);
        if (portfolios.findByUserId(userId).size() >= MAX_PORTFOLIOS_PER_USER) {
            throw new IllegalArgumentException("You can have at most " + MAX_PORTFOLIOS_PER_USER + " portfolios");
        }
        return portfolios.create(userId, validName, INITIAL_CASH);
    }

    public Portfolio createDefault(long userId) {
        return portfolios.create(userId, DEFAULT_NAME, INITIAL_CASH);
    }

    public Portfolio rename(long userId, long portfolioId, String name) {
        String validName = validateName(name);
        get(userId, portfolioId);
        return portfolios.rename(portfolioId, validName)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));
    }

    /** Deletes the SQL Server portfolio first; leftover Oracle trades of a missing portfolio are unreachable. */
    public void delete(long userId, long portfolioId) {
        get(userId, portfolioId);
        portfolios.delete(portfolioId);
        try {
            trades.deleteByPortfolioId(portfolioId);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Portfolio " + portfolioId + " was deleted but its trades could not be removed",
                    exception);
        }
    }

    private static String validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Portfolio name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Portfolio name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }
}
