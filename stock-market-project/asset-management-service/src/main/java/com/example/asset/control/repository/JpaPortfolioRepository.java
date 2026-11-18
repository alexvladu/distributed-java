package com.example.asset.control.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/** Portfolios live in SQL Server; {@code user_id} is a logical reference to PostgreSQL users. */
@Stateless
public class JpaPortfolioRepository implements PortfolioRepository {
    @PersistenceContext(unitName = "sqlserver")
    private EntityManager entityManager;

    @Override
    public Portfolio create(long userId, String name, BigDecimal cashBalance) {
        PortfolioEntity portfolio = new PortfolioEntity(userId, name, cashBalance);
        entityManager.persist(portfolio);
        entityManager.flush();
        // Reads created_at, which SQL Server filled in.
        entityManager.refresh(portfolio);
        return portfolio.toPortfolio();
    }

    @Override
    public Optional<Portfolio> findById(long id) {
        return Optional.ofNullable(entityManager.find(PortfolioEntity.class, id)).map(PortfolioEntity::toPortfolio);
    }

    @Override
    public List<Portfolio> findByUserId(long userId) {
        return entityManager.createQuery(
                        "SELECT p FROM PortfolioEntity p WHERE p.userId = :userId ORDER BY p.portfolioId",
                        PortfolioEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream()
                .map(PortfolioEntity::toPortfolio)
                .toList();
    }

    @Override
    public Optional<Portfolio> rename(long id, String name) {
        PortfolioEntity portfolio = entityManager.find(PortfolioEntity.class, id);
        if (portfolio == null) {
            return Optional.empty();
        }
        portfolio.rename(name);
        return Optional.of(portfolio.toPortfolio());
    }

    @Override
    public void delete(long id) {
        PortfolioEntity portfolio = entityManager.find(PortfolioEntity.class, id);
        if (portfolio != null) {
            entityManager.remove(portfolio);
        }
    }

    /** The row lock (WITH (UPDLOCK)) makes the balance check and the update one atomic step. */
    @Override
    public boolean adjustCash(long id, BigDecimal delta) {
        PortfolioEntity portfolio = entityManager.find(PortfolioEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        return portfolio != null && portfolio.adjustCash(delta);
    }
}
