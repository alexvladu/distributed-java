package com.example.stock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public final class StockCompanyRepository {
    private final ConcurrentMap<String, StockCompany> companies = new ConcurrentHashMap<>();

    public StockCompanyRepository() {
        save(new StockCompany("AAPL", "Apple Inc.", new BigDecimal("190.25"), 12));
        save(new StockCompany("MSFT", "Microsoft Corporation", new BigDecimal("425.50"), 8));
        save(new StockCompany("TSLA", "Tesla Inc.", new BigDecimal("245.10"), 5));
        save(new StockCompany("AMZN", "Amazon.com Inc.", new BigDecimal("185.00"), 10));
        save(new StockCompany("NVDA", "NVIDIA Corporation", new BigDecimal("138.75"), 15));
    }

    public void save(StockCompany company) {
        companies.put(company.symbol(), company);
    }

    public List<StockCompany> findAll() {
        List<StockCompany> result = new ArrayList<>(companies.values());
        result.sort(Comparator.comparing(StockCompany::symbol));
        return result;
    }

    public StockCompany createRandomPriceUpdate() {
        List<StockCompany> available = findAll();
        StockCompany current = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        double movement = ThreadLocalRandom.current().nextGaussian() * 0.0025;
        BigDecimal updatedPrice = current.price()
                .multiply(BigDecimal.valueOf(1 + movement))
                .max(new BigDecimal("0.01"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        StockCompany updated = new StockCompany(
                current.symbol(), current.name(), updatedPrice, current.quantity());
        return updated;
    }
}
