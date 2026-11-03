package com.example.asset.control.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.asset.entity.portfolio.Portfolio;

import jakarta.ejb.Local;

@Local
public interface PortfolioRepository {
    Portfolio create(long userId, String name, BigDecimal cashBalance);

    Optional<Portfolio> findById(long id);

    List<Portfolio> findByUserId(long userId);

    Optional<Portfolio> rename(long id, String name);

    void delete(long id);

    /**
     * Atomically adds {@code delta} to the cash balance.
     *
     * @return false when the portfolio does not exist or the balance would become negative
     */
    boolean adjustCash(long id, BigDecimal delta);
}
