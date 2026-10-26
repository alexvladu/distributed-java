package com.example.stock;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HealthServlet extends HttpServlet {
    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StockMarketSimulator simulator = (StockMarketSimulator) getServletContext()
                .getAttribute(StockMarketSimulator.class.getName());
        StockMarketSimulator.Health health = simulator == null
                ? new StockMarketSimulator.Health("DOWN", 0, null, "Simulator is not initialized")
                : simulator.health();
        response.setStatus("DOWN".equals(health.status())
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                : HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSON.writeValue(response.getWriter(), health);
    }
}
