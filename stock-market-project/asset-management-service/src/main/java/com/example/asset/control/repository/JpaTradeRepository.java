package com.example.asset.control.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeEntity;
import com.example.asset.entity.trade.TradeSide;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Trades live in Oracle; {@code portfolio_id} is a logical reference to SQL Server portfolios. */
@Stateless
public class JpaTradeRepository implements TradeRepository {
    @PersistenceContext(unitName = "oracle")
    private EntityManager entityManager;

    @Override
    public Trade create(long portfolioId, String symbol, TradeSide side, BigDecimal quantity, BigDecimal price) {
        TradeEntity trade = new TradeEntity(portfolioId, symbol, side, quantity, price);
        entityManager.persist(trade);
        entityManager.flush();
        // Reads executed_at, which Oracle filled in.
        entityManager.refresh(trade);
        return trade.toTrade();
    }

    @Override
    public Optional<Trade> findById(long id) {
        return Optional.ofNullable(entityManager.find(TradeEntity.class, id)).map(TradeEntity::toTrade);
    }

    @Override
    public List<Trade> findByPortfolioId(long portfolioId) {
        return entityManager.createQuery("""
                        SELECT t FROM TradeEntity t WHERE t.portfolioId = :portfolioId
                        ORDER BY t.executedAt DESC, t.tradeId DESC""", TradeEntity.class)
                .setParameter("portfolioId", portfolioId)
                .getResultList().stream()
                .map(TradeEntity::toTrade)
                .toList();
    }

    @Override
    public Optional<Trade> updateQuantity(long id, BigDecimal quantity) {
        TradeEntity trade = entityManager.find(TradeEntity.class, id);
        if (trade == null) {
            return Optional.empty();
        }
        trade.changeQuantity(quantity);
        return Optional.of(trade.toTrade());
    }

    @Override
    public void delete(long id) {
        TradeEntity trade = entityManager.find(TradeEntity.class, id);
        if (trade != null) {
            entityManager.remove(trade);
        }
    }

    @Override
    public void deleteByPortfolioId(long portfolioId) {
        entityManager.createQuery("DELETE FROM TradeEntity t WHERE t.portfolioId = :portfolioId")
                .setParameter("portfolioId", portfolioId)
                .executeUpdate();
    }
}
