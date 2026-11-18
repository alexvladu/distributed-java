package com.example.asset.entity.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * SQL Server {@code dbo.portfolios} row (persistence unit {@code sqlserver}). {@code userId} is a logical
 * reference to PostgreSQL {@code users}, since a foreign key cannot span database engines.
 */
@Entity
@Table(name = "portfolios", schema = "dbo")
public class PortfolioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "portfolio_name")
    private String portfolioName;

    @Column(name = "cash_balance")
    private BigDecimal cashBalance;

    /** UTC time filled in by SQL Server ({@code DEFAULT SYSUTCDATETIME()} on a {@code DATETIME2} column). */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PortfolioEntity() {
    }

    public PortfolioEntity(long userId, String portfolioName, BigDecimal cashBalance) {
        this.userId = userId;
        this.portfolioName = portfolioName;
        this.cashBalance = cashBalance;
    }

    public void rename(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    /** Adds {@code delta} to the cash balance unless the balance would become negative. */
    public boolean adjustCash(BigDecimal delta) {
        BigDecimal next = cashBalance.add(delta);
        if (next.signum() < 0) {
            return false;
        }
        cashBalance = next;
        return true;
    }

    public Portfolio toPortfolio() {
        return new Portfolio(portfolioId, userId, portfolioName, cashBalance, createdAt.toInstant(ZoneOffset.UTC));
    }
}
