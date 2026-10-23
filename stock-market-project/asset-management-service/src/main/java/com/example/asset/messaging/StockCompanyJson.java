package com.example.asset.messaging;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StockCompanyJson {
    private static final Pattern JSON = Pattern.compile(
            "\\{\"symbol\":\"(.*?)\",\"name\":\"(.*?)\",\"price\":([0-9]+(?:\\.[0-9]+)?),\"quantity\":([0-9]+)\\}");

    private StockCompanyJson() {
    }

    public static StockCompany read(String value) {
        Matcher matcher = JSON.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid StockCompany JSON");
        }
        return new StockCompany(unescape(matcher.group(1)), unescape(matcher.group(2)),
                new BigDecimal(matcher.group(3)), Long.parseLong(matcher.group(4)));
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
