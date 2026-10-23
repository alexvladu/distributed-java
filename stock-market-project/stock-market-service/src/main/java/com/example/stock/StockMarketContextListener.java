package com.example.stock;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class StockMarketContextListener implements ServletContextListener {
    private StockMarketPublisher publisher;
    private StockMarketSimulator simulator;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        publisher = new StockMarketPublisher();
        simulator = new StockMarketSimulator(new StockCompanyRepository(), publisher);
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
}
