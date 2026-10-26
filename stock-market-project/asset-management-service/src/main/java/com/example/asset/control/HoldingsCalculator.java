package com.example.asset.control;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeSide;

/** Derives open positions from the trade ledger using the moving average cost method. */
public final class HoldingsCalculator {
    private HoldingsCalculator() {
    }

    public record Position(String symbol, BigDecimal quantity, BigDecimal costBasis) {
        public BigDecimal averageCost() {
            return quantity.signum() == 0
                    ? BigDecimal.ZERO
                    : costBasis.divide(quantity, 4, RoundingMode.HALF_UP);
        }
    }

    private static final class Lot {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
    }

    /** Returns open positions sorted by symbol. Trades may be passed in any order. */
    public static List<Position> positions(List<Trade> trades) {
        List<Trade> chronological = new ArrayList<>(trades);
        chronological.sort(Comparator.comparingLong(Trade::tradeId));
        Map<String, Lot> lots = new TreeMap<>();
        for (Trade trade : chronological) {
            Lot lot = lots.computeIfAbsent(trade.symbol(), symbol -> new Lot());
            BigDecimal price = trade.price() == null ? BigDecimal.ZERO : trade.price();
            if (trade.side() == TradeSide.BUY) {
                lot.quantity = lot.quantity.add(trade.quantity());
                lot.cost = lot.cost.add(price.multiply(trade.quantity()));
            } else if (lot.quantity.signum() > 0) {
                BigDecimal remaining = lot.quantity.subtract(trade.quantity().min(lot.quantity));
                lot.cost = remaining.signum() == 0
                        ? BigDecimal.ZERO
                        : lot.cost.multiply(remaining).divide(lot.quantity, 10, RoundingMode.HALF_UP);
                lot.quantity = remaining;
            }
        }
        List<Position> positions = new ArrayList<>();
        lots.forEach((symbol, lot) -> {
            if (lot.quantity.signum() > 0) {
                positions.add(new Position(symbol, lot.quantity.stripTrailingZeros(), lot.cost));
            }
        });
        return positions;
    }

    public static BigDecimal quantityOf(List<Trade> trades, String symbol) {
        return positions(trades).stream()
                .filter(position -> position.symbol().equals(symbol))
                .map(Position::quantity)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    /** True when no sale in the ledger sells more shares than were held at that point. */
    public static boolean isConsistent(List<Trade> trades) {
        List<Trade> chronological = new ArrayList<>(trades);
        chronological.sort(Comparator.comparingLong(Trade::tradeId));
        Map<String, BigDecimal> held = new HashMap<>();
        for (Trade trade : chronological) {
            BigDecimal current = held.getOrDefault(trade.symbol(), BigDecimal.ZERO);
            if (trade.side() == TradeSide.BUY) {
                held.put(trade.symbol(), current.add(trade.quantity()));
            } else if (current.compareTo(trade.quantity()) < 0) {
                return false;
            } else {
                held.put(trade.symbol(), current.subtract(trade.quantity()));
            }
        }
        return true;
    }
}
