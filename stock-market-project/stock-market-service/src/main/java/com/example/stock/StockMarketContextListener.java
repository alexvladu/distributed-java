package com.example.stock;

import com.example.stock.contract.StockPriceEvent;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/** Registered in WEB-INF/web.xml because the Jetty base does not enable annotation scanning. */
public class StockMarketContextListener implements ServletContextListener {
    private StockMarketPublisher publisher;
    private StockMarketSimulator simulator;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        publisher = new StockMarketPublisher(
                env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
                env("KAFKA_TOPIC", StockPriceEvent.DEFAULT_TOPIC));
        simulator = new StockMarketSimulator(new StockCompanyRepository(), publisher);
        event.getServletContext().setAttribute(StockMarketSimulator.class.getName(), simulator);
        simulator.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (simulator != null) {
            simulator.close();
        }
        if (publisher != null) {
            publisher.close();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
