package com.example.asset.boundary;

import com.example.asset.application.ApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/api/*")
public final class CrudServlet extends HttpServlet {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] path = path(request);
            long userId = authenticatedUserId(request);
            Object result = switch (path[0]) {
                case "users" -> ApplicationContext.users().find(userId);
                case "portfolios" -> path.length == 1
                        ? ApplicationContext.portfolios().findByUser(userId)
                        : ApplicationContext.portfolios().find(Long.parseLong(path[1]));
                case "trades" -> path.length == 1
                        ? ApplicationContext.trades().findByPortfolio(userId,
                                requiredLong(request, "portfolioId"))
                        : ApplicationContext.trades().find(Long.parseLong(path[1]));
                default -> throw new IllegalArgumentException("Unknown resource");
            };
            write(response, result);
        } catch (RuntimeException exception) {
            error(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] path = path(request);
            JsonNode body = json.readTree(request.getInputStream());
            Object result = switch (path[0]) {
                case "users" -> ApplicationContext.auth().register(text(body, "username"),
                        text(body, "email"), text(body, "password"));
                case "portfolios" -> ApplicationContext.portfolios().create(
                        authenticatedUserId(request), text(body, "portfolioName"));
                case "trades" -> ApplicationContext.trades().create(
                        authenticatedUserId(request), number(body, "portfolioId"), text(body, "symbol"),
                        text(body, "side"), decimal(body, "quantity"));
                default -> throw new IllegalArgumentException("Unknown resource");
            };
            response.setStatus(HttpServletResponse.SC_CREATED);
            write(response, result);
        } catch (RuntimeException exception) {
            error(response, exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] path = path(request);
            if (path.length != 2) throw new IllegalArgumentException("An ID is required");
            JsonNode body = json.readTree(request.getInputStream());
            Object result = switch (path[0]) {
                case "users" -> ApplicationContext.users().update(authenticatedUserId(request), Long.parseLong(path[1]),
                        text(body, "username"), text(body, "email"));
                case "portfolios" -> ApplicationContext.portfolios().update(authenticatedUserId(request), Long.parseLong(path[1]),
                        text(body, "portfolioName"));
                case "trades" -> ApplicationContext.trades().update(authenticatedUserId(request), Long.parseLong(path[1]),
                        text(body, "symbol"), text(body, "side"), decimal(body, "quantity"));
                default -> throw new IllegalArgumentException("Unknown resource");
            };
            write(response, result);
        } catch (RuntimeException exception) {
            error(response, exception);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] path = path(request);
            if (path.length != 2) throw new IllegalArgumentException("An ID is required");
            long id = Long.parseLong(path[1]);
            switch (path[0]) {
                case "users" -> ApplicationContext.users().delete(authenticatedUserId(request), id);
                case "portfolios" -> ApplicationContext.portfolios().delete(authenticatedUserId(request), id);
                case "trades" -> ApplicationContext.trades().delete(authenticatedUserId(request), id);
                default -> throw new IllegalArgumentException("Unknown resource");
            }
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (RuntimeException exception) {
            error(response, exception);
        }
    }

    private String[] path(HttpServletRequest request) {
        String value = request.getPathInfo();
        if (value == null || value.length() < 2) throw new IllegalArgumentException("Resource is required");
        return value.substring(1).split("/");
    }
    private long requiredLong(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return Long.parseLong(value);
    }
    private long authenticatedUserId(HttpServletRequest request) {
        Object value = request.getSession(false).getAttribute("authenticatedUserId");
        if (!(value instanceof Long)) throw new IllegalStateException("Invalid authenticated session");
        return (Long) value;
    }
    private String text(JsonNode body, String name) {
        JsonNode value = body == null ? null : body.get(name);
        if (value == null || value.isNull() || value.asText().isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.asText();
    }
    private long number(JsonNode body, String name) {
        JsonNode value = body == null ? null : body.get(name);
        if (value == null || !value.canConvertToLong()) throw new IllegalArgumentException(name + " must be a number");
        return value.longValue();
    }
    private BigDecimal decimal(JsonNode body, String name) {
        JsonNode value = body == null ? null : body.get(name);
        if (value == null || !value.isNumber()) throw new IllegalArgumentException(name + " must be a number");
        return value.decimalValue();
    }
    private void write(HttpServletResponse response, Object value) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), value);
    }
    private void error(HttpServletResponse response, RuntimeException exception) throws IOException {
        int status = exception instanceof SecurityException ? 403 : exception instanceof IllegalArgumentException ? 400 : 500;
        response.setStatus(status);
        write(response, Map.of("error", exception.getMessage() == null ? "Request failed" : exception.getMessage()));
    }
}
