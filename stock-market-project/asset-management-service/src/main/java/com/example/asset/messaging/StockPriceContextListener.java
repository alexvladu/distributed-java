package com.example.asset.messaging;

import com.example.asset.portfolio.PortfolioService;

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
        event.getServletContext().setAttribute(PortfolioService.class.getName(), new PortfolioService());
        consumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (consumer != null) {
            consumer.close();
        }
    }
}
