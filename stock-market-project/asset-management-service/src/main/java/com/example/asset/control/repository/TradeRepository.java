package com.example.asset.control.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeSide;

import jakarta.ejb.Local;

@Local
public interface TradeRepository {
    Trade create(long portfolioId, String symbol, TradeSide side, BigDecimal quantity, BigDecimal price);

    Optional<Trade> findById(long id);

    /** Newest first. */
    List<Trade> findByPortfolioId(long portfolioId);

    Optional<Trade> updateQuantity(long id, BigDecimal quantity);

    void delete(long id);

    void deleteByPortfolioId(long portfolioId);
}
