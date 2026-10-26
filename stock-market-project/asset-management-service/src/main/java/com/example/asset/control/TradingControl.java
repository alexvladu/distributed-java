package com.example.asset.control;

import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import com.example.asset.entity.portfolio.Portfolio;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.Trade;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.trade.TradeSide;

/**
 * Executes market orders across two databases: cash in SQL Server, the trade ledger in Oracle.
 * Cash moves first with a conditional update; if recording the trade fails, the cash movement is reversed.
 */
public final class TradingControl {
    private static final System.Logger LOG = System.getLogger(TradingControl.class.getName());
    private static final int MAX_QUANTITY_SCALE = 6;
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000000");
    private static final Pattern SYMBOL = Pattern.compile("[A-Z0-9.\\-]{1,12}");
    private static final int LOCK_STRIPES = 64;

    public record Execution(Trade trade, AccountSnapshot account) {
    }

    private final PortfolioControl portfolioControl;
    private final PortfolioRepository portfolios;
    private final TradeRepository trades;
    private final MarketControl market;
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public TradingControl(PortfolioControl portfolioControl, PortfolioRepository portfolios,
            TradeRepository trades, MarketControl market) {
        this.portfolioControl = portfolioControl;
        this.portfolios = portfolios;
        this.trades = trades;
        this.market = market;
        for (int index = 0; index < LOCK_STRIPES; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    public AccountSnapshot account(long userId, long portfolioId) {
        Portfolio portfolio = portfolioControl.get(userId, portfolioId);
        return snapshot(portfolio, trades.findByPortfolioId(portfolioId));
    }

    public List<Trade> history(long userId, long portfolioId) {
        portfolioControl.get(userId, portfolioId);
        return trades.findByPortfolioId(portfolioId);
    }

    public Trade trade(long userId, long tradeId) {
        Trade trade = trades.findById(tradeId).orElseThrow(() -> new NotFoundException("Trade not found"));
        try {
            portfolioControl.get(userId, trade.portfolioId());
        } catch (NotFoundException exception) {
            throw new NotFoundException("Trade not found");
        }
        return trade;
    }

    public Execution execute(long userId, long portfolioId, String symbol, String side, BigDecimal quantity) {
        TradeSide tradeSide = parseSide(side);
        String validSymbol = parseSymbol(symbol);
        BigDecimal validQuantity = validateQuantity(quantity);
        portfolioControl.get(userId, portfolioId);
        StockQuote quote = market.quote(validSymbol).orElseThrow(
                () -> new IllegalArgumentException(validSymbol + " is not available in the live market"));
        BigDecimal price = quote.price();
        BigDecimal amount = price.multiply(validQuantity).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Order value must be at least $0.01");
        }

        // Serializes orders per portfolio so two sells cannot both pass the holdings check.
        ReentrantLock lock = lockFor(portfolioId);
        lock.lock();
        try {
            if (tradeSide == TradeSide.SELL) {
                BigDecimal owned = HoldingsCalculator.quantityOf(trades.findByPortfolioId(portfolioId), validSymbol);
                if (owned.compareTo(validQuantity) < 0) {
                    throw new IllegalArgumentException("Insufficient shares: you own "
                            + owned.toPlainString() + " " + validSymbol);
                }
            }
            BigDecimal cashDelta = tradeSide == TradeSide.BUY ? amount.negate() : amount;
            moveCash(portfolioId, cashDelta, "Insufficient cash for this purchase");
            Trade trade;
            try {
                trade = trades.create(portfolioId, validSymbol, tradeSide, validQuantity, price);
            } catch (RuntimeException exception) {
                throw reverseCash(portfolioId, cashDelta, exception);
            }
            return new Execution(trade, snapshotAfterTrade(userId, portfolioId));
        } finally {
            lock.unlock();
        }
    }

    /** Corrects the quantity of a recorded trade at its original price and settles the cash difference. */
    public Execution correct(long userId, long tradeId, BigDecimal quantity) {
        BigDecimal validQuantity = validateQuantity(quantity);
        long portfolioId = trade(userId, tradeId).portfolioId();
        ReentrantLock lock = lockFor(portfolioId);
        lock.lock();
        try {
            Trade current = trades.findById(tradeId).orElseThrow(() -> new NotFoundException("Trade not found"));
            Trade corrected = new Trade(current.tradeId(), current.portfolioId(), current.symbol(), current.side(),
                    validQuantity, current.price(), current.executedAt());
            List<Trade> ledger = trades.findByPortfolioId(portfolioId).stream()
                    .map(trade -> trade.tradeId() == tradeId ? corrected : trade)
                    .toList();
            if (!HoldingsCalculator.isConsistent(ledger)) {
                throw new IllegalArgumentException("This change would leave a later sale without enough shares");
            }
            BigDecimal cashDelta = cashEffect(corrected).subtract(cashEffect(current));
            moveCash(portfolioId, cashDelta, "Insufficient cash for this correction");
            Trade updated;
            try {
                updated = trades.updateQuantity(tradeId, validQuantity)
                        .orElseThrow(() -> new IllegalStateException("Trade " + tradeId + " disappeared"));
            } catch (RuntimeException exception) {
                throw reverseCash(portfolioId, cashDelta, exception);
            }
            return new Execution(updated, snapshotAfterTrade(userId, portfolioId));
        } finally {
            lock.unlock();
        }
    }

    /** Removes a trade from the ledger and reverses its cash effect. */
    public void voidTrade(long userId, long tradeId) {
        long portfolioId = trade(userId, tradeId).portfolioId();
        ReentrantLock lock = lockFor(portfolioId);
        lock.lock();
        try {
            Trade current = trades.findById(tradeId).orElseThrow(() -> new NotFoundException("Trade not found"));
            List<Trade> ledger = trades.findByPortfolioId(portfolioId).stream()
                    .filter(trade -> trade.tradeId() != tradeId)
                    .toList();
            if (!HoldingsCalculator.isConsistent(ledger)) {
                throw new IllegalArgumentException("Voiding this buy would leave a later sale without enough shares");
            }
            BigDecimal cashDelta = cashEffect(current).negate();
            moveCash(portfolioId, cashDelta, "Insufficient cash to reverse this sale");
            try {
                trades.delete(tradeId);
            } catch (RuntimeException exception) {
                throw reverseCash(portfolioId, cashDelta, exception);
            }
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lockFor(long portfolioId) {
        return locks[Math.floorMod(Long.hashCode(portfolioId), LOCK_STRIPES)];
    }

    /** Cash movement caused by a trade: buys spend, sells earn; legacy trades without a price move nothing. */
    private static BigDecimal cashEffect(Trade trade) {
        if (trade.price() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = trade.price().multiply(trade.quantity()).setScale(2, RoundingMode.HALF_UP);
        return trade.side() == TradeSide.BUY ? amount.negate() : amount;
    }

    private void moveCash(long portfolioId, BigDecimal delta, String insufficientMessage) {
        if (delta.signum() == 0) {
            return;
        }
        if (!portfolios.adjustCash(portfolioId, delta)) {
            if (delta.signum() < 0) {
                throw new IllegalArgumentException(insufficientMessage);
            }
            throw new NotFoundException("Portfolio not found");
        }
    }

    AccountSnapshot snapshot(Portfolio portfolio, List<Trade> ledger) {
        List<HoldingView> holdings = new ArrayList<>();
        BigDecimal investedValue = BigDecimal.ZERO;
        BigDecimal pricedCostBasis = BigDecimal.ZERO;
        for (HoldingsCalculator.Position position : HoldingsCalculator.positions(ledger)) {
            Optional<StockQuote> quote = market.quote(position.symbol());
            BigDecimal costBasis = position.costBasis().setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = quote.map(StockQuote::price).orElse(null);
            BigDecimal marketValue = null;
            BigDecimal profit = null;
            if (price != null) {
                marketValue = price.multiply(position.quantity()).setScale(2, RoundingMode.HALF_UP);
                profit = marketValue.subtract(costBasis);
                investedValue = investedValue.add(marketValue);
                pricedCostBasis = pricedCostBasis.add(costBasis);
            }
            holdings.add(new HoldingView(position.symbol(), quote.map(StockQuote::name).orElse(position.symbol()),
                    position.quantity(), position.averageCost(), costBasis, price, marketValue, profit));
        }
        BigDecimal cash = portfolio.cashBalance().setScale(2, RoundingMode.HALF_UP);
        return new AccountSnapshot(portfolio.portfolioId(), portfolio.portfolioName(), cash, investedValue,
                pricedCostBasis, investedValue.subtract(pricedCostBasis), cash.add(investedValue), holdings);
    }

    /** The trade is already committed, so a failing read must not turn the response into an error. */
    private AccountSnapshot snapshotAfterTrade(long userId, long portfolioId) {
        try {
            return account(userId, portfolioId);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Trade recorded but the account snapshot could not be loaded", exception);
            return null;
        }
    }

    /** Undoes a cash movement after the Oracle write failed, and returns the exception to throw. */
    private UnavailableException reverseCash(long portfolioId, BigDecimal cashDelta, RuntimeException cause) {
        boolean reversed = false;
        try {
            reversed = cashDelta.signum() == 0 || portfolios.adjustCash(portfolioId, cashDelta.negate());
        } catch (RuntimeException exception) {
            cause.addSuppressed(exception);
        }
        if (reversed) {
            LOG.log(Level.WARNING, "Trade ledger write failed for portfolio " + portfolioId
                    + "; cash movement " + cashDelta.toPlainString() + " was reversed", cause);
            return new UnavailableException(
                    "The trade ledger could not be updated, so the cash movement was reversed. Please try again.",
                    cause);
        }
        LOG.log(Level.ERROR, "MANUAL RECONCILIATION REQUIRED: portfolio " + portfolioId
                + " cash moved by " + cashDelta.toPlainString() + " but the trade ledger was not updated", cause);
        return new UnavailableException(
                "The trade ledger could not be updated and the cash movement could not be reversed. "
                        + "Contact support before trading again.", cause);
    }

    private static TradeSide parseSide(String side) {
        if (side != null) {
            String normalized = side.trim().toUpperCase(Locale.ROOT);
            for (TradeSide candidate : TradeSide.values()) {
                if (candidate.name().equals(normalized)) {
                    return candidate;
                }
            }
        }
        throw new IllegalArgumentException("side must be BUY or SELL");
    }

    private static String parseSymbol(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("A valid stock symbol is required");
        }
        return normalized;
    }

    private static BigDecimal validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        BigDecimal normalized = quantity.stripTrailingZeros();
        if (normalized.scale() > MAX_QUANTITY_SCALE) {
            throw new IllegalArgumentException("Quantity supports at most " + MAX_QUANTITY_SCALE + " decimal places");
        }
        if (normalized.compareTo(MAX_QUANTITY) > 0) {
            throw new IllegalArgumentException("Quantity is too large");
        }
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }
}
