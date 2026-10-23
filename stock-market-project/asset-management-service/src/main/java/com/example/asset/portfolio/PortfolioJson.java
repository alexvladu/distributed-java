package com.example.asset.portfolio;

import java.util.stream.Collectors;

public final class PortfolioJson {
    private PortfolioJson() {
    }

    public static String write(AccountSnapshot account) {
        String holdings = account.holdings().stream()
                .map(holding -> "{\"symbol\":\"" + escape(holding.symbol()) + "\","
                        + "\"name\":\"" + escape(holding.name()) + "\","
                        + "\"quantity\":" + holding.quantity().toPlainString() + ","
                        + "\"averagePrice\":" + holding.averagePrice().toPlainString() + "}")
                .collect(Collectors.joining(","));
        return "{\"cashBalance\":" + account.cashBalance().toPlainString()
                + ",\"investedValue\":" + account.investedValue().toPlainString()
                + ",\"totalValue\":" + account.totalValue().toPlainString()
                + ",\"holdings\":[" + holdings + "]}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
