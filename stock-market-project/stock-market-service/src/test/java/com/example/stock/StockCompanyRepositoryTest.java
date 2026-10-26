package com.example.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class StockCompanyRepositoryTest {
    @Test
    void randomUpdatesKeepKnownSymbolsAndValidPrices() {
        StockCompanyRepository repository = new StockCompanyRepository();
        Set<String> symbols = repository.findAll().stream().map(StockCompany::symbol).collect(Collectors.toSet());

        for (int index = 0; index < 1_000; index++) {
            StockCompany updated = repository.createRandomPriceUpdate();
            assertTrue(symbols.contains(updated.symbol()));
            assertTrue(updated.price().compareTo(new BigDecimal("0.01")) >= 0, updated.toString());
            assertEquals(2, updated.price().scale());
            repository.save(updated);
        }

        assertEquals(symbols.size(), repository.findAll().size());
    }

    @Test
    void findAllIsSortedBySymbol() {
        var symbols = new StockCompanyRepository().findAll().stream().map(StockCompany::symbol).toList();

        assertEquals(symbols.stream().sorted().toList(), symbols);
    }
}
