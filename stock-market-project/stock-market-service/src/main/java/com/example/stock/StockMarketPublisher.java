package com.example.stock;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.example.stock.contract.StockPriceEvent;
import com.example.stock.contract.StockPriceEventCodec;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public final class StockMarketPublisher implements AutoCloseable {
    private static final long SEND_TIMEOUT_SECONDS = 30;

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public StockMarketPublisher(String bootstrapServers, String topic) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        // Fail fast instead of blocking the simulator thread for the 60s default when Kafka is down.
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000");
        this.producer = new KafkaProducer<>(properties);
        this.topic = topic;
    }

    public void publish(StockCompany company, Instant timestamp)
            throws InterruptedException, ExecutionException, TimeoutException {
        StockPriceEvent event = new StockPriceEvent(company.symbol(), company.name(), company.price(),
                company.quantity(), timestamp);
        producer.send(new ProducerRecord<>(topic, company.symbol(), StockPriceEventCodec.encode(event)))
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        producer.close(Duration.ofSeconds(5));
    }
}
