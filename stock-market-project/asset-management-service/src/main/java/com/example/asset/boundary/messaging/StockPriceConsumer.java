package com.example.asset.boundary.messaging;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.asset.control.MarketControl;
import com.example.stock.contract.StockPriceEvent;
import com.example.stock.contract.StockPriceEventCodec;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Inbound Kafka boundary. Every start replays the (compacted) topic from the beginning so the market view is
 * complete immediately, and a malformed message is skipped instead of stopping the consumer.
 */
public final class StockPriceConsumer implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger(StockPriceConsumer.class.getName());
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private final Properties properties;
    private final String topic;
    private final MarketControl market;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final Thread worker;
    private volatile KafkaConsumer<String, String> consumer;

    public StockPriceConsumer(String bootstrapServers, String topic, MarketControl market) {
        this.topic = topic;
        this.market = market;
        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // A unique group per instance: every instance needs every price, and nothing is committed.
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "asset-management-service-" + UUID.randomUUID());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        worker = new Thread(this::run, "stock-price-consumer");
        worker.setDaemon(true);
    }

    public void start() {
        worker.start();
    }

    private void run() {
        while (running.get()) {
            try (KafkaConsumer<String, String> kafka = new KafkaConsumer<>(properties)) {
                consumer = kafka;
                kafka.subscribe(List.of(topic), new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        // Nothing to commit: offsets are never stored.
                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        kafka.seekToBeginning(partitions);
                    }
                });
                while (running.get()) {
                    for (ConsumerRecord<String, String> message : kafka.poll(Duration.ofMillis(500))) {
                        handle(message);
                    }
                }
            } catch (WakeupException exception) {
                // close() was called.
            } catch (RuntimeException exception) {
                if (running.get()) {
                    LOG.log(Level.ERROR, "Kafka consumer failed; reconnecting in " + RETRY_DELAY.toSeconds() + "s",
                            exception);
                    awaitRetry();
                }
            } finally {
                consumer = null;
            }
        }
    }

    private void handle(ConsumerRecord<String, String> message) {
        try {
            StockPriceEvent event = StockPriceEventCodec.decode(message.value());
            Instant timestamp = event.timestamp() != null
                    ? event.timestamp()
                    : Instant.ofEpochMilli(message.timestamp());
            market.apply(event.symbol(), event.name(), event.price(), timestamp);
            LOG.log(Level.DEBUG, "Received {0} at {1}", event.symbol(), event.price().toPlainString());
        } catch (IllegalArgumentException exception) {
            LOG.log(Level.WARNING, "Skipping malformed stock price message at offset " + message.offset()
                    + ": " + exception.getMessage());
        }
    }

    private void awaitRetry() {
        try {
            if (stopped.await(RETRY_DELAY.toMillis(), TimeUnit.MILLISECONDS)) {
                running.set(false);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    @Override
    public void close() {
        running.set(false);
        stopped.countDown();
        KafkaConsumer<String, String> active = consumer;
        if (active != null) {
            active.wakeup();
        }
        try {
            worker.join(5_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
