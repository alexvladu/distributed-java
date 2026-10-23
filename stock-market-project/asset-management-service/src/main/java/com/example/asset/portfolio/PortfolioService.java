package com.example.asset.portfolio;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.example.asset.messaging.StockCompany;

public final class PortfolioService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    private final Path accountFile;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private BigDecimal cashBalance;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();

    public PortfolioService() {
        this(Path.of(System.getenv().getOrDefault(
                "ACCOUNT_DATA_FILE", "src/main/resources/data/account.json")));
    }

    PortfolioService(Path accountFile) {
        this.accountFile = accountFile;
        load();
    }

    public synchronized AccountSnapshot snapshot(Map<String, StockCompany> market) {
        BigDecimal investedValue = holdings.values().stream()
                .map(holding -> {
                    StockCompany stock = market.get(holding.symbol());
                    return stock == null
                            ? BigDecimal.ZERO
                            : stock.price().multiply(holding.quantity());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new AccountSnapshot(
                cashBalance,
                investedValue,
                cashBalance.add(investedValue).setScale(2, RoundingMode.HALF_UP),
                List.copyOf(holdings.values()));
    }

    public synchronized AccountSnapshot buy(StockCompany stock, BigDecimal quantity,
            Map<String, StockCompany> market) {
        validateQuantity(quantity);
        StockCompany currentStock = market.get(stock.symbol());
        if (currentStock == null) {
            throw new IllegalArgumentException("Stock is not available in the live market");
        }

        BigDecimal cost = currentStock.price().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        if (cost.compareTo(cashBalance) > 0) {
            throw new IllegalArgumentException("Insufficient balance for this purchase");
        }

        Holding previous = holdings.get(currentStock.symbol());
        BigDecimal totalQuantity = previous == null
                ? quantity
                : previous.quantity().add(quantity);
        holdings.put(currentStock.symbol(), new Holding(
                currentStock.symbol(), currentStock.name(), totalQuantity));
        cashBalance = cashBalance.subtract(cost).setScale(2, RoundingMode.HALF_UP);
        save();
        return snapshot(market);
    }

    public synchronized AccountSnapshot sell(StockCompany stock, BigDecimal quantity,
            Map<String, StockCompany> market) {
        validateQuantity(quantity);
        StockCompany currentStock = market.get(stock.symbol());
        if (currentStock == null) {
            throw new IllegalArgumentException("Stock is not available in the live market");
        }

        Holding holding = holdings.get(currentStock.symbol());
        if (holding == null || holding.quantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient shares for this sale");
        }

        BigDecimal proceeds = currentStock.price().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingQuantity = holding.quantity().subtract(quantity);
        if (remainingQuantity.signum() == 0) {
            holdings.remove(currentStock.symbol());
        } else {
            holdings.put(currentStock.symbol(), new Holding(
                    holding.symbol(), holding.name(), remainingQuantity));
        }
        cashBalance = cashBalance.add(proceeds).setScale(2, RoundingMode.HALF_UP);
        save();
        return snapshot(market);
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private void load() {
        cashBalance = INITIAL_BALANCE;
        if (!Files.exists(accountFile)) {
            return;
        }
        try {
            String json = Files.readString(accountFile, StandardCharsets.UTF_8);
            AccountState state = objectMapper.readValue(json, AccountState.class);
            if (state.cashBalance() == null) {
                throw new IllegalStateException("Account file is missing cashBalance");
            }
            cashBalance = state.cashBalance().setScale(2, RoundingMode.HALF_UP);
            if (state.holdings() != null) {
                state.holdings().forEach(holding -> holdings.put(holding.symbol(), holding));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load account file: " + accountFile, exception);
        }
    }

    private void save() {
        try {
            Path parent = accountFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporaryFile = accountFile.resolveSibling(accountFile.getFileName() + ".tmp");
            Files.writeString(temporaryFile, objectMapper.writeValueAsString(
                    new AccountState(cashBalance, sortedHoldings())), StandardCharsets.UTF_8);
            Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to save account file: " + accountFile, exception);
        }
    }

    private List<Holding> sortedHoldings() {
        List<Holding> sortedHoldings = new ArrayList<>(holdings.values());
        sortedHoldings.sort(Comparator.comparing(Holding::symbol));
        return sortedHoldings;
    }
}
