package com.example.asset.boundary.api;

import java.io.IOException;
import java.util.List;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.control.MarketControl;
import com.example.asset.error.NotFoundException;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <pre>
 * GET /api/market            all stocks and their prices
 * GET /api/market/{symbol}   one stock
 * </pre>
 */
@WebServlet("/api/market/*")
public final class MarketServlet extends ApiServlet {
    @EJB
    private MarketControl market;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            List<String> path = segments(request);
            if (path.isEmpty()) {
                Json.write(response, HttpServletResponse.SC_OK, market.quotes());
            } else if (path.size() == 1) {
                Json.write(response, HttpServletResponse.SC_OK, market.quote(path.get(0))
                        .orElseThrow(() -> new NotFoundException("Unknown symbol")));
            } else {
                notFound(response);
            }
        });
    }
}
