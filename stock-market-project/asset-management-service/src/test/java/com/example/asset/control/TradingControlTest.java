package com.example.asset.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.asset.control.InMemoryRepositories.Portfolios;
import com.example.asset.control.InMemoryRepositories.Trades;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeSide;

class TradingControlTest {
    private static final long ALICE = 1;
    private static final long BOB = 2;
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final Portfolios portfolios = new Portfolios();
    private final Trades trades = new Trades();
    private final MarketControl market = new MarketControl();
    private final PortfolioControl portfolioControl = new PortfolioControl(portfolios, trades);
    private final TradingControl trading = new TradingControl(portfolioControl, portfolios, trades, market);
    private long portfolioId;

    @BeforeEach
    void setUp() {
        portfolioId = portfolioControl.createDefault(ALICE).portfolioId();
        market.apply("AAPL", "Apple Inc.", new BigDecimal("100.00"), START);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void buyDebitsCashAndRecordsTheTradeAtTheMarketPrice() {
        TradingControl.Execution execution = trading.execute(ALICE, portfolioId, " aapl ", "buy",
                new BigDecimal("2.5"));

        assertEquals("AAPL", execution.trade().symbol());
        assertEquals(TradeSide.BUY, execution.trade().side());
        assertAmount("100.00", execution.trade().price());
        assertAmount("750.00", portfolios.cash(portfolioId));
        AccountSnapshot account = execution.account();
        assertAmount("750.00", account.cashBalance());
        assertAmount("250.00", account.investedValue());
        assertAmount("1000.00", account.totalValue());
        assertEquals(1, account.holdings().size());
    }

    @Test
    void buyWithoutEnoughCashIsRejectedWithoutSideEffects() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> trading.execute(ALICE, portfolioId, "AAPL", "BUY", new BigDecimal("10.01")));

        assertTrue(error.getMessage().contains("Insufficient cash"), error.getMessage());
        assertAmount("1000.00", portfolios.cash(portfolioId));
        assertTrue(trades.rows.isEmpty());
    }

    @Test
    void sellCreditsProceedsAndKeepsTheAverageCost() {
        trading.execute(ALICE, portfolioId, "AAPL", "BUY", new BigDecimal("3"));
        market.apply("AAPL", "Apple Inc.", new BigDecimal("110.00"), START.plusSeconds(10));

        trading.execute(ALICE, portfolioId, "AAPL", "SELL", BigDecimal.ONE);

        assertAmount("810.00", portfolios.cash(portfolioId));
        HoldingView holding = trading.account(ALICE, portfolioId).holdings().get(0);
        assertAmount("2", holding.quantity());
        assertAmount("100", holding.averageCost());
        assertAmount("220.00", holding.marketValue());
        assertAmount("20.00", holding.unrealizedProfit());
    }

    @Test
    void cannotSellMoreThanOwned() {
        trading.execute(ALICE, portfolioId, "AAPL", "BUY", BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class,
                () -> trading.execute(ALICE, portfolioId, "AAPL", "SELL", new BigDecimal("1.5")));
        assertAmount("900.00", portfolios.cash(portfolioId));
        assertEquals(1, trades.rows.size());
    }

    @Test
    void reversesTheCashMovementWhenTheTradeCannotBeRecorded() {
        trades.createFailure = new IllegalStateException("Oracle is unavailable");

        assertThrows(UnavailableException.class,
                () -> trading.execute(ALICE, portfolioId, "AAPL", "BUY", BigDecimal.ONE));
        assertAmount("1000.00", portfolios.cash(portfolioId));
    }

    @Test
    void portfoliosAndTradesOfOtherUsersAreInvisible() {
        Trade trade = trading.execute(ALICE, portfolioId, "AAPL", "BUY", BigDecimal.ONE).trade();

        assertThrows(NotFoundException.class,
                () -> trading.execute(BOB, portfolioId, "AAPL", "BUY", BigDecimal.ONE));
        assertThrows(NotFoundException.class, () -> trading.account(BOB, portfolioId));
        assertThrows(NotFoundException.class, () -> trading.history(BOB, portfolioId));
        assertThrows(NotFoundException.class, () -> trading.trade(BOB, trade.tradeId()));
        assertEquals(trade, trading.trade(ALICE, trade.tradeId()));
    }

    @Test
    void correctingABuySettlesTheCashDifferenceAtTheOriginalPrice() {
        Trade trade = trading.execute(ALICE, portfolioId, "AAPL", "BUY", new BigDecimal("2")).trade();
        market.apply("AAPL", "Apple Inc.", new BigDecimal("150.00"), START.plusSeconds(10));

        Trade corrected = trading.correct(ALICE, trade.tradeId(), new BigDecimal("3")).trade();

        assertAmount("3", corrected.quantity());
        assertAmount("100.00", corrected.price());
        assertAmount("700.00", portfolios.cash(portfolioId));
    }

    @Test
    void voidingATradeReversesItsCashEffect() {
        Trade trade = trading.execute(ALICE, portfolioId, "AAPL", "BUY", new BigDecimal("2")).trade();

        trading.voidTrade(ALICE, trade.tradeId());

        assertAmount("1000.00", portfolios.cash(portfolioId));
        assertTrue(trades.rows.isEmpty());
    }

    @Test
    void ledgerChangesThatWouldBreakALaterSaleAreRejected() {
        Trade buy = trading.execute(ALICE, portfolioId, "AAPL", "BUY", new BigDecimal("2")).trade();
        trading.execute(ALICE, portfolioId, "AAPL", "SELL", new BigDecimal("2"));

        assertThrows(IllegalArgumentException.class, () -> trading.voidTrade(ALICE, buy.tradeId()));
        assertThrows(IllegalArgumentException.class, () -> trading.correct(ALICE, buy.tradeId(), BigDecimal.ONE));
        assertThrows(NotFoundException.class, () -> trading.voidTrade(BOB, buy.tradeId()));
        assertAmount("1000.00", portfolios.cash(portfolioId));
        assertEquals(2, trades.rows.size());
    }

    @Test
    void rejectsInvalidOrders() {
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", null));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", new BigDecimal("0.0000001")));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", new BigDecimal("2000000000")));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "BUY", new BigDecimal("0.000001")));
        assertThrows(IllegalArgumentException.class, () -> order("AAPL", "HOLD", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> order("AA PL", "BUY", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> order("MSFT", "BUY", BigDecimal.ONE));
        assertTrue(trades.rows.isEmpty());
        assertAmount("1000.00", portfolios.cash(portfolioId));
    }

    private void order(String symbol, String side, BigDecimal quantity) {
        trading.execute(ALICE, portfolioId, symbol, side, quantity);
    }
}
