package com.example.asset.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

public final class StockPriceConsumer implements AutoCloseable {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Properties consumerProperties;
    private volatile KafkaConsumer<String, String> consumer;
    private final Thread worker;
    private final Map<String, StockCompany> latestPrices = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastUpdated = new ConcurrentHashMap<>();

    public StockPriceConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "asset-management-service");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties = properties;
        worker = new Thread(this::consume, "stock-price-consumer");
        worker.setDaemon(true);
    }

    public void start() {
        worker.start();
    }

    private void consume() {
        KafkaConsumer<String, String> workerConsumer = null;
        try {
            workerConsumer = new KafkaConsumer<>(consumerProperties);
            consumer = workerConsumer;
            workerConsumer.subscribe(Collections.singleton(
                    System.getenv().getOrDefault("KAFKA_TOPIC", "stock-prices")));
            while (running.get()) {
                for (ConsumerRecord<String, String> record : workerConsumer.poll(Duration.ofMillis(500))) {
                    StockCompany company = StockCompanyJson.read(record.value());
                    latestPrices.put(company.symbol(), company);
                    lastUpdated.put(company.symbol(), Instant.now());
                    System.out.printf("Received stock price for %s: %s at %s (quantity %d)%n",
                            company.symbol(), company.name(), company.price(), company.quantity());
                }
            }
        } catch (WakeupException exception) {
            if (running.get()) {
                throw exception;
            }
        } catch (RuntimeException exception) {
            System.err.println("Stock price consumer stopped: " + exception.getMessage());
        } finally {
            if (workerConsumer != null) {
                workerConsumer.close();
            }
            consumer = null;
        }
    }

    public List<StockCompany> latestPrices() {
        List<StockCompany> result = new ArrayList<>(latestPrices.values());
        result.sort(Comparator.comparing(StockCompany::symbol));
        return result;
    }

    public Instant lastUpdated(String symbol) {
        return lastUpdated.get(symbol);
    }

    @Override
    public void close() {
        running.set(false);
        KafkaConsumer<String, String> activeConsumer = consumer;
        if (activeConsumer != null) {
            activeConsumer.wakeup();
        }
        try {
            worker.join(5_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
