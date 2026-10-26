package com.example.stock;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class StockMarketSimulator implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger(StockMarketSimulator.class.getName());
    private static final int MIN_UPDATE_SECONDS = 5;
    private static final int MAX_UPDATE_SECONDS = 15;
    private static final Duration HEALTHY_PUBLISH_WINDOW = Duration.ofSeconds(60);

    public record Health(String status, int symbols, Instant lastPublishedAt, String lastError) {
    }

    private final StockCompanyRepository repository;
    private final StockMarketPublisher publisher;
    private final ScheduledThreadPoolExecutor executor;
    private volatile Instant lastPublishedAt;
    private volatile String lastError;

    public StockMarketSimulator(StockCompanyRepository repository, StockMarketPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
        this.executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "stock-market-simulator");
            thread.setDaemon(true);
            return thread;
        });
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /** Publishes the initial catalog asynchronously so a slow broker does not block server startup. */
    public void start() {
        executor.execute(() -> {
            repository.findAll().forEach(this::publish);
            scheduleNextUpdate();
        });
    }

    public Health health() {
        Instant published = lastPublishedAt;
        String status;
        if (published != null && published.isAfter(Instant.now().minus(HEALTHY_PUBLISH_WINDOW))) {
            status = "UP";
        } else if (published == null && lastError == null) {
            status = "STARTING";
        } else {
            status = "DOWN";
        }
        return new Health(status, repository.findAll().size(), published, lastError);
    }

    private boolean publish(StockCompany stock) {
        try {
            publisher.publish(stock, Instant.now());
            lastPublishedAt = Instant.now();
            lastError = null;
            LOG.log(Level.INFO, "Published {0} at {1}", stock.symbol(), stock.price().toPlainString());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            lastError = "Publishing interrupted";
            return false;
        } catch (Exception exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            LOG.log(Level.WARNING, "Unable to publish " + stock.symbol() + ": " + lastError);
            return false;
        }
    }

    private void scheduleNextUpdate() {
        if (executor.isShutdown()) {
            return;
        }
        int delay = ThreadLocalRandom.current().nextInt(MIN_UPDATE_SECONDS, MAX_UPDATE_SECONDS + 1);
        try {
            executor.schedule(this::updateAndReschedule, delay, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
            // The simulator is shutting down.
        }
    }

    private void updateAndReschedule() {
        try {
            StockCompany updated = repository.createRandomPriceUpdate();
            if (publish(updated)) {
                repository.save(updated);
            }
        } catch (RuntimeException exception) {
            LOG.log(Level.ERROR, "Unexpected simulator failure", exception);
        } finally {
            scheduleNextUpdate();
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
