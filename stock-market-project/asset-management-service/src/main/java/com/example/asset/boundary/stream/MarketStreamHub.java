package com.example.asset.boundary.stream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.asset.boundary.http.Json;
import com.example.asset.control.MarketControl;
import com.example.asset.control.StockQuote;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Server-Sent Events fan-out for market quotes. Each browser gets its own bounded, ordered queue drained by a
 * virtual thread, so a slow client can only drop its own oldest frames and never blocks the Kafka thread.
 */
public final class MarketStreamHub implements AutoCloseable {
    private static final long HEARTBEAT_SECONDS = 20;
    private static final int CLIENT_QUEUE_CAPACITY = 64;

    private final MarketControl market;
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();
    private final Consumer<StockQuote> listener = this::broadcast;
    private final ScheduledExecutorService heartbeat;

    public MarketStreamHub(MarketControl market) {
        this.market = market;
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "market-stream-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleAtFixedRate(() -> clients.forEach(client -> client.send(() -> ": heartbeat\n\n")),
                HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        market.subscribe(listener);
    }

    public void open(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        AsyncContext async = request.startAsync();
        async.setTimeout(0);
        Client client = new Client(async, response.getWriter());
        async.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                client.close();
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                client.close();
            }

            @Override
            public void onError(AsyncEvent event) {
                client.close();
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                // Not re-dispatched, so there is nothing to re-register.
            }
        });
        clients.add(client);
        // Built when it is written, so it is never older than quotes queued before it.
        client.send(() -> "retry: 5000\n" + frame("snapshot", market.quotes()));
    }

    public int clientCount() {
        return clients.size();
    }

    private void broadcast(StockQuote quote) {
        if (clients.isEmpty()) {
            return;
        }
        String frame = frame("quote", quote);
        clients.forEach(client -> client.send(() -> frame));
    }

    private static String frame(String event, Object data) {
        return "event: " + event + "\ndata: " + Json.stringify(data) + "\n\n";
    }

    @Override
    public void close() {
        market.unsubscribe(listener);
        heartbeat.shutdownNow();
        clients.forEach(Client::close);
    }

    private final class Client {
        private final AsyncContext async;
        private final PrintWriter writer;
        private final ThreadPoolExecutor executor;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean open = true;

        private Client(AsyncContext async, PrintWriter writer) {
            this.async = async;
            this.writer = writer;
            this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(CLIENT_QUEUE_CAPACITY),
                    Thread.ofVirtual().name("market-stream-client").factory(),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
        }

        private void send(Supplier<String> frame) {
            if (!open) {
                return;
            }
            try {
                executor.execute(() -> write(frame));
            } catch (RejectedExecutionException ignored) {
                // Closed concurrently.
            }
        }

        private void write(Supplier<String> frame) {
            lock.lock();
            try {
                if (!open) {
                    return;
                }
                writer.write(frame.get());
                writer.flush();
                if (writer.checkError()) {
                    close();
                }
            } catch (RuntimeException exception) {
                close();
            } finally {
                lock.unlock();
            }
        }

        /** Never writes after completing: the container may recycle the response afterwards. */
        private void close() {
            lock.lock();
            try {
                if (!open) {
                    return;
                }
                open = false;
            } finally {
                lock.unlock();
            }
            clients.remove(this);
            executor.shutdownNow();
            try {
                async.complete();
            } catch (RuntimeException ignored) {
                // Already completed by the container.
            }
        }
    }
}
