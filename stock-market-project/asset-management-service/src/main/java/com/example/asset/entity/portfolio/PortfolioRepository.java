package com.example.asset.entity.portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
    Portfolio create(long userId, String name);
    Optional<Portfolio> findById(long id);
    List<Portfolio> findByUserId(long userId);
    Portfolio update(long id, String name);
    void delete(long id);
}
