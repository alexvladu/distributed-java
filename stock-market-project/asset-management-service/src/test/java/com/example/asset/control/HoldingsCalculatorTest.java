package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.asset.control.HoldingsCalculator.Position;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeSide;

class HoldingsCalculatorTest {
    private static Trade trade(long id, String symbol, TradeSide side, String quantity, String price) {
        return new Trade(id, 1, symbol, side, new BigDecimal(quantity), price == null ? null : new BigDecimal(price),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void usesTheMovingAverageCost() {
        List<Position> positions = HoldingsCalculator.positions(List.of(
                trade(1, "AAPL", TradeSide.BUY, "2", "100"),
                trade(2, "AAPL", TradeSide.BUY, "2", "200"),
                trade(3, "AAPL", TradeSide.SELL, "1", "500")));

        assertEquals(1, positions.size());
        Position position = positions.get(0);
        assertAmount("3", position.quantity());
        assertAmount("450", position.costBasis());
        assertAmount("150", position.averageCost());
    }

    @Test
    void ordersTradesByIdAndDropsClosedPositions() {
        List<Position> positions = HoldingsCalculator.positions(List.of(
                trade(4, "MSFT", TradeSide.BUY, "0.5", "400"),
                trade(2, "AAPL", TradeSide.SELL, "1", "120"),
                trade(1, "AAPL", TradeSide.BUY, "1", "100")));

        assertEquals(1, positions.size());
        assertEquals("MSFT", positions.get(0).symbol());
        assertAmount("200", positions.get(0).costBasis());
    }

    @Test
    void sortsPositionsBySymbol() {
        List<Position> positions = HoldingsCalculator.positions(List.of(
                trade(1, "TSLA", TradeSide.BUY, "1", "10"),
                trade(2, "AAPL", TradeSide.BUY, "1", "10")));

        assertEquals(List.of("AAPL", "TSLA"), positions.stream().map(Position::symbol).toList());
    }

    @Test
    void legacyTradesWithoutPriceHaveZeroCost() {
        Position position = HoldingsCalculator.positions(List.of(trade(1, "AAPL", TradeSide.BUY, "1", null))).get(0);

        assertAmount("1", position.quantity());
        assertAmount("0", position.costBasis());
        assertAmount("0", position.averageCost());
    }

    @Test
    void detectsSalesThatExceedTheSharesHeldAtThatTime() {
        assertTrue(HoldingsCalculator.isConsistent(List.of(
                trade(1, "AAPL", TradeSide.BUY, "2", "100"),
                trade(2, "AAPL", TradeSide.SELL, "2", "100"))));
        assertFalse(HoldingsCalculator.isConsistent(List.of(
                trade(1, "AAPL", TradeSide.SELL, "1", "100"),
                trade(2, "AAPL", TradeSide.BUY, "2", "100"))));
    }

    @Test
    void quantityOfUnknownSymbolIsZero() {
        assertAmount("0", HoldingsCalculator.quantityOf(List.of(), "AAPL"));
        assertTrue(HoldingsCalculator.positions(List.of()).isEmpty());
    }
}
