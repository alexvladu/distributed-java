package com.example.asset.web;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.example.asset.messaging.StockCompany;
import com.example.asset.messaging.StockPriceConsumer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/live-market/*")
public class LiveMarketServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StockPriceConsumer consumer = getConsumer();
        if ("data".equals(request.getPathInfo())) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(toJson(consumer.latestPrices(), consumer));
            return;
        }

        request.setAttribute("stocks", consumer.latestPrices());
        request.setAttribute("consumer", consumer);
        request.getRequestDispatcher("/WEB-INF/views/live-market.jsp").forward(request, response);
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

    private String toJson(List<StockCompany> stocks, StockPriceConsumer consumer) {
        return stocks.stream()
                .map(stock -> "{\"symbol\":\"" + escape(stock.symbol()) + "\","
                        + "\"name\":\"" + escape(stock.name()) + "\","
                        + "\"price\":" + stock.price().toPlainString() + ","
                        + "\"quantity\":" + stock.quantity() + ","
                        + "\"updatedAt\":\"" + consumer.lastUpdated(stock.symbol()) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
