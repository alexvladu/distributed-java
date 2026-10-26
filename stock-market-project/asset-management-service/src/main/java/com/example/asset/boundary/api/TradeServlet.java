package com.example.asset.boundary.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;
import com.example.asset.control.NotFoundException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <pre>
 * GET    /api/trades?portfolioId={id}   history of a portfolio (prefer /api/portfolios/{id}/trades)
 * GET    /api/trades/{id}               one trade
 * POST   /api/trades                    market order {portfolioId, symbol, side, quantity}
 * PUT    /api/trades/{id}               correct the quantity {quantity}; cash is settled at the original price
 * DELETE /api/trades/{id}               void the trade and reverse its cash effect
 * </pre>
 */
@WebServlet("/api/trades/*")
public final class TradeServlet extends ApiServlet {
    public record TradeRequest(Long portfolioId, String symbol, String side, BigDecimal quantity) {
    }

    public record CorrectionRequest(BigDecimal quantity) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            if (path.isEmpty()) {
                String portfolioId = request.getParameter("portfolioId");
                if (portfolioId == null) {
                    throw new IllegalArgumentException("portfolioId is required");
                }
                Json.write(response, HttpServletResponse.SC_OK, app().trading().history(userId, id(portfolioId)));
            } else if (path.size() == 1) {
                Json.write(response, HttpServletResponse.SC_OK, app().trading().trade(userId, id(path.get(0))));
            } else {
                notFound(response);
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            if (!segments(request).isEmpty()) {
                notFound(response);
                return;
            }
            TradeRequest body = Json.read(request, TradeRequest.class);
            if (body.portfolioId() == null) {
                throw new IllegalArgumentException("portfolioId is required");
            }
            Json.write(response, HttpServletResponse.SC_CREATED, app().trading().execute(
                    userId, body.portfolioId(), body.symbol(), body.side(), body.quantity()));
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            long tradeId = requireSingleId(request);
            CorrectionRequest body = Json.read(request, CorrectionRequest.class);
            Json.write(response, HttpServletResponse.SC_OK,
                    app().trading().correct(userId, tradeId, body.quantity()));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            app().trading().voidTrade(userId, requireSingleId(request));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        });
    }

    private static long requireSingleId(HttpServletRequest request) {
        List<String> path = segments(request);
        if (path.size() != 1) {
            throw new NotFoundException("Trade not found");
        }
        return id(path.get(0));
    }
}
