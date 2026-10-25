package com.example.asset.entity.trade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TradeRepository {
    Trade create(long portfolioId, String symbol, String side, BigDecimal quantity);
    Optional<Trade> findById(long id);
    List<Trade> findByPortfolioId(long portfolioId);
    Trade update(long id, String symbol, String side, BigDecimal quantity);
    void delete(long id);
}
