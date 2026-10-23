package com.example.stock;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class StockMarketSimulator implements AutoCloseable {
    private static final int MIN_UPDATE_SECONDS = 5;
    private static final int MAX_UPDATE_SECONDS = 15;

    private final StockCompanyRepository repository;
    private final StockMarketPublisher publisher;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> nextUpdate;

    public StockMarketSimulator(StockCompanyRepository repository, StockMarketPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "stock-market-simulator");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        publishInitialStocks();
        scheduleNextUpdate();
    }

    private void publishInitialStocks() {
        for (StockCompany stock : repository.findAll()) {
            try {
                publisher.publish(stock);
                System.out.printf("Published initial stock: %s at %s%n",
                        stock.symbol(), stock.price());
            } catch (Exception exception) {
                System.err.printf("Unable to publish initial stock %s: %s%n",
                        stock.symbol(), exception.getMessage());
            }
        }
    }

    private void scheduleNextUpdate() {
        int delay = ThreadLocalRandom.current().nextInt(
                MIN_UPDATE_SECONDS, MAX_UPDATE_SECONDS + 1);
        nextUpdate = executor.schedule(this::updateAndReschedule, delay, TimeUnit.SECONDS);
    }

    private void updateAndReschedule() {
        try {
            StockCompany updated = repository.createRandomPriceUpdate();
            publisher.publish(updated);
            repository.save(updated);
            System.out.printf("Published stock update: %s at %s%n",
                    updated.symbol(), updated.price());
        } catch (Exception exception) {
            System.err.println("Unable to publish stock update: " + exception.getMessage());
        } finally {
            if (!executor.isShutdown()) {
                scheduleNextUpdate();
            }
        }
    }

    @Override
    public void close() {
        if (nextUpdate != null) {
            nextUpdate.cancel(false);
        }
        executor.shutdownNow();
    }
}
