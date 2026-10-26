package com.example.asset.boundary.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;
import com.example.asset.control.TradingControl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <pre>
 * GET    /api/portfolios                 list (SQL Server)
 * POST   /api/portfolios                 create {portfolioName}
 * GET    /api/portfolios/{id}            read
 * PUT    /api/portfolios/{id}            rename {portfolioName}
 * DELETE /api/portfolios/{id}            delete with its trades
 * GET    /api/portfolios/{id}/account    cash, holdings, and valuation
 * GET    /api/portfolios/{id}/trades     trade history (Oracle)
 * POST   /api/portfolios/{id}/trades     market order {symbol, side, quantity}
 * </pre>
 */
@WebServlet("/api/portfolios/*")
public final class PortfolioServlet extends ApiServlet {
    public record PortfolioRequest(String portfolioName) {
    }

    public record OrderRequest(String symbol, String side, BigDecimal quantity) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            Object result;
            if (path.isEmpty()) {
                result = app().portfolios().list(userId);
            } else if (path.size() == 1) {
                result = app().portfolios().get(userId, id(path.get(0)));
            } else if (path.size() == 2 && "account".equals(path.get(1))) {
                result = app().trading().account(userId, id(path.get(0)));
            } else if (path.size() == 2 && "trades".equals(path.get(1))) {
                result = app().trading().history(userId, id(path.get(0)));
            } else {
                notFound(response);
                return;
            }
            Json.write(response, HttpServletResponse.SC_OK, result);
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            if (path.isEmpty()) {
                PortfolioRequest body = Json.read(request, PortfolioRequest.class);
                Json.write(response, HttpServletResponse.SC_CREATED,
                        app().portfolios().create(userId, body.portfolioName()));
            } else if (path.size() == 2 && "trades".equals(path.get(1))) {
                long portfolioId = id(path.get(0));
                OrderRequest body = Json.read(request, OrderRequest.class);
                TradingControl.Execution execution = app().trading().execute(
                        userId, portfolioId, body.symbol(), body.side(), body.quantity());
                Json.write(response, HttpServletResponse.SC_CREATED, execution);
            } else {
                notFound(response);
            }
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            if (path.size() != 1) {
                notFound(response);
                return;
            }
            PortfolioRequest body = Json.read(request, PortfolioRequest.class);
            Json.write(response, HttpServletResponse.SC_OK,
                    app().portfolios().rename(userId, id(path.get(0)), body.portfolioName()));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            if (path.size() != 1) {
                notFound(response);
                return;
            }
            app().portfolios().delete(userId, id(path.get(0)));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        });
    }
}
