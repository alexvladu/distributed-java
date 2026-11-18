package com.example.asset.entity.trade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Oracle {@code trades} row (persistence unit {@code oracle}). {@code portfolioId} is a logical reference to
 * SQL Server {@code portfolios}, since a foreign key cannot span database engines.
 */
@Entity
@Table(name = "trades")
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(name = "portfolio_id")
    private long portfolioId;

    @Column(name = "symbol")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @Column(name = "quantity")
    private BigDecimal quantity;

    /** Execution price per share; null only for trades recorded before prices were tracked. */
    @Column(name = "price")
    private BigDecimal price;

    /** Filled in by Oracle ({@code DEFAULT SYSTIMESTAMP}). */
    @Column(name = "executed_at", insertable = false, updatable = false)
    private OffsetDateTime executedAt;

    protected TradeEntity() {
    }

    public TradeEntity(long portfolioId, String symbol, TradeSide side, BigDecimal quantity, BigDecimal price) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public void changeQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Trade toTrade() {
        return new Trade(tradeId, portfolioId, symbol, side, quantity, price, executedAt);
    }
}
