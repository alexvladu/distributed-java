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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.asset.messaging.StockCompany;

public final class PortfolioService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private static final Pattern CASH = Pattern.compile("\"cashBalance\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern HOLDING = Pattern.compile(
            "\\{\"symbol\":\"(.*?)\",\"name\":\"(.*?)\",\"quantity\":([0-9]+(?:\\.[0-9]+)?),"
                    + "\"averagePrice\":([0-9]+(?:\\.[0-9]+)?)\\}");

    private final Path accountFile;
    private BigDecimal cashBalance;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();

    public PortfolioService() {
        this(Path.of(System.getenv().getOrDefault("ACCOUNT_DATA_FILE", "data/account.json")));
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
                            ? holding.averagePrice().multiply(holding.quantity())
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
        BigDecimal totalCost = previous == null
                ? cost
                : previous.averagePrice().multiply(previous.quantity()).add(cost);
        BigDecimal averagePrice = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
        holdings.put(currentStock.symbol(), new Holding(
                currentStock.symbol(), currentStock.name(), totalQuantity, averagePrice));
        cashBalance = cashBalance.subtract(cost).setScale(2, RoundingMode.HALF_UP);
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
            Matcher cashMatcher = CASH.matcher(json);
            if (!cashMatcher.find()) {
                throw new IllegalStateException("Account file is missing cashBalance");
            }
            cashBalance = new BigDecimal(cashMatcher.group(1)).setScale(2, RoundingMode.HALF_UP);
            Matcher holdingMatcher = HOLDING.matcher(json);
            while (holdingMatcher.find()) {
                Holding holding = new Holding(
                        unescape(holdingMatcher.group(1)),
                        unescape(holdingMatcher.group(2)),
                        new BigDecimal(holdingMatcher.group(3)),
                        new BigDecimal(holdingMatcher.group(4)));
                holdings.put(holding.symbol(), holding);
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
            Files.writeString(temporaryFile, toJson(), StandardCharsets.UTF_8);
            Files.move(temporaryFile, accountFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save account file: " + accountFile, exception);
        }
    }

    private String toJson() {
        List<Holding> sortedHoldings = new ArrayList<>(holdings.values());
        sortedHoldings.sort(Comparator.comparing(Holding::symbol));
        String holdingJson = sortedHoldings.stream()
                .map(holding -> "{\"symbol\":\"" + escape(holding.symbol()) + "\","
                        + "\"name\":\"" + escape(holding.name()) + "\","
                        + "\"quantity\":" + holding.quantity().toPlainString() + ","
                        + "\"averagePrice\":" + holding.averagePrice().toPlainString() + "}")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "{\"cashBalance\":" + cashBalance.toPlainString()
                + ",\"holdings\":[" + holdingJson + "]}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
