package com.example.asset.web;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import com.example.asset.messaging.StockCompany;
import com.example.asset.messaging.StockPriceConsumer;
import com.example.asset.portfolio.PortfolioJson;
import com.example.asset.portfolio.PortfolioService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(value = "/assets/*", asyncSupported = true)
public class AssetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StockPriceConsumer consumer = getConsumer();
        PortfolioService portfolio = getPortfolio();
        if ("/events".equals(request.getPathInfo())) {
            openEventStream(request, response, consumer, portfolio);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/assets.jsp").forward(request, response);
    }

    private void openEventStream(HttpServletRequest request, HttpServletResponse response,
            StockPriceConsumer consumer, PortfolioService portfolio) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        var asyncContext = request.startAsync();
        asyncContext.setTimeout(0);
        Consumer<StockCompany> listener = stock -> {
            try {
                response.getWriter().print("data: " + marketJson(consumer, portfolio) + "\n\n");
                response.getWriter().flush();
            } catch (IOException exception) {
                asyncContext.complete();
            }
        };
        asyncContext.addListener(new jakarta.servlet.AsyncListener() {
            @Override
            public void onComplete(jakarta.servlet.AsyncEvent event) {
                consumer.removeListener(listener);
            }

            @Override
            public void onTimeout(jakarta.servlet.AsyncEvent event) {
                consumer.removeListener(listener);
            }

            @Override
            public void onError(jakarta.servlet.AsyncEvent event) {
                consumer.removeListener(listener);
            }

            @Override
            public void onStartAsync(jakarta.servlet.AsyncEvent event) {
            }
        });
        consumer.addListener(listener);
        response.getWriter().print("data: " + marketJson(consumer, portfolio) + "\n\n");
        response.getWriter().flush();
    }

    private String marketJson(StockPriceConsumer consumer, PortfolioService portfolio) {
        Map<String, StockCompany> market = consumer.latestPricesBySymbol();
        String stocks = consumer.latestPrices().stream()
                .map(stock -> "{\"symbol\":\"" + escape(stock.symbol()) + "\","
                        + "\"name\":\"" + escape(stock.name()) + "\","
                        + "\"price\":" + stock.price().toPlainString() + ","
                        + "\"updatedAt\":\"" + consumer.lastUpdated(stock.symbol()) + "\"}")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"stocks\":[" + stocks + "],\"account\":"
                + PortfolioJson.write(portfolio.snapshot(market)) + "}";
    }

    private StockPriceConsumer getConsumer() throws ServletException {
        StockPriceConsumer consumer = (StockPriceConsumer) getServletContext().getAttribute(
                StockPriceConsumer.class.getName());
        if (consumer == null) {
            throw new ServletException("Stock price consumer is not initialized");
        }
        return consumer;
    }

    private PortfolioService getPortfolio() throws ServletException {
        PortfolioService portfolio = (PortfolioService) getServletContext().getAttribute(
                PortfolioService.class.getName());
        if (portfolio == null) {
            throw new ServletException("Portfolio service is not initialized");
        }
        return portfolio;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
