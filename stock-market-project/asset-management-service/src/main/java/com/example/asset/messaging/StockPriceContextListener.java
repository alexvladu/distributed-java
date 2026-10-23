package com.example.asset.messaging;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class StockPriceContextListener implements ServletContextListener {
    private StockPriceConsumer consumer;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        consumer = new StockPriceConsumer();
        event.getServletContext().setAttribute(StockPriceConsumer.class.getName(), consumer);
        consumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (consumer != null) {
            consumer.close();
        }
    }
}
