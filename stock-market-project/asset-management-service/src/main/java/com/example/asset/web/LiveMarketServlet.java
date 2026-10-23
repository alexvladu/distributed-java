package com.example.asset.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import com.example.asset.messaging.StockCompany;
import com.example.asset.messaging.StockPriceConsumer;
import com.example.asset.portfolio.AccountSnapshot;
import com.example.asset.portfolio.PortfolioJson;
import com.example.asset.portfolio.PortfolioService;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(value = "/live-market/*", asyncSupported = true)
public class LiveMarketServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StockPriceConsumer consumer = getConsumer();
        Map<String, StockCompany> market = consumer.latestPricesBySymbol();
        AccountSnapshot account = getPortfolio().snapshot(market);
        if ("/events".equals(request.getPathInfo())) {
            openEventStream(request, response, consumer);
            return;
        }
        if ("/data".equals(request.getPathInfo())) {
            writeJson(response, marketJson(consumer.latestPrices(), consumer, account));
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/live-market.jsp").forward(request, response);
    }

    private void openEventStream(HttpServletRequest request, HttpServletResponse response,
            StockPriceConsumer consumer) throws IOException, ServletException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        var asyncContext = request.startAsync();
        asyncContext.setTimeout(0);
        PortfolioService portfolio = getPortfolio();
        Consumer<StockCompany> listener = stock -> {
            try {
                Map<String, StockCompany> market = consumer.latestPricesBySymbol();
                AccountSnapshot account = portfolio.snapshot(market);
                response.getWriter().print("data: " + marketEventJson(stock, consumer, account) + "\n\n");
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
        response.getWriter().print(": connected\n\n");
        response.getWriter().flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/buy".equals(request.getPathInfo())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        StockPriceConsumer consumer = getConsumer();
        Map<String, StockCompany> market = consumer.latestPricesBySymbol();
        String symbol = request.getParameter("symbol");
        StockCompany stock = symbol == null ? null : market.get(symbol.trim().toUpperCase());
        if (stock == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Stock is not available in the live market");
            return;
        }
        try {
            String quantityParameter = request.getParameter("quantity");
            if (quantityParameter == null || quantityParameter.isBlank()) {
                throw new IllegalArgumentException("Quantity is required");
            }
            BigDecimal quantity = new BigDecimal(quantityParameter);
            AccountSnapshot account = getPortfolio().buy(stock, quantity, market);
            writeJson(response, PortfolioJson.write(account));
        } catch (NumberFormatException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Quantity must be a valid number");
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ServletException("Unable to save account", exception);
        }
    }

    private StockPriceConsumer getConsumer() throws ServletException {
        ServletContext context = getServletContext();
        StockPriceConsumer consumer = (StockPriceConsumer) context.getAttribute(
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

    private String marketJson(List<StockCompany> stocks, StockPriceConsumer consumer,
            AccountSnapshot account) {
        String stockJson = stocks.stream()
                .map(stock -> "{\"symbol\":\"" + escape(stock.symbol()) + "\","
                        + "\"name\":\"" + escape(stock.name()) + "\","
                        + "\"price\":" + stock.price().toPlainString() + ","
                        + "\"quantity\":" + stock.quantity() + ","
                        + "\"updatedAt\":\"" + consumer.lastUpdated(stock.symbol()) + "\"}")
                .collect(Collectors.joining(","));
        return "{\"stocks\":[" + stockJson + "],\"account\":" + PortfolioJson.write(account) + "}";
    }

    private String marketEventJson(StockCompany stock, StockPriceConsumer consumer,
            AccountSnapshot account) {
        return "{\"stock\":{\"symbol\":\"" + escape(stock.symbol()) + "\","
                + "\"name\":\"" + escape(stock.name()) + "\","
                + "\"price\":" + stock.price().toPlainString() + ","
                + "\"quantity\":" + stock.quantity() + ","
                + "\"updatedAt\":\"" + consumer.lastUpdated(stock.symbol()) + "\"},"
                + "\"account\":" + PortfolioJson.write(account) + "}";
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(json);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().printf("{\"error\":\"%s\"}", escape(message));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
