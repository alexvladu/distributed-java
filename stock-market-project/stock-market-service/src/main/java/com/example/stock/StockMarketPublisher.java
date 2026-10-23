package com.example.stock;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public final class StockMarketPublisher implements AutoCloseable {
    public static final String TOPIC = System.getenv().getOrDefault("KAFKA_TOPIC", "stock-prices");

    private final KafkaProducer<String, String> producer;

    public StockMarketPublisher() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers",
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());
        properties.put("acks", "all");
        producer = new KafkaProducer<>(properties);
    }

    public RecordMetadata publish(StockCompany company) throws Exception {
        Future<RecordMetadata> result = producer.send(
                new ProducerRecord<>(TOPIC, company.symbol(), StockCompanyJson.write(company)));
        return result.get();
    }

    @Override
    public void close() {
        producer.close();
    }
}
