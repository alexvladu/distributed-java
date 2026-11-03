package com.example.asset.boundary.api;

import java.io.IOException;
import java.util.List;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.stockevents.MarketStreamHub;
import com.example.asset.control.MarketControl;
import com.example.asset.control.NotFoundException;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <pre>
 * GET /api/market            all quotes
 * GET /api/market/{symbol}   one quote
 * GET /api/market/stream     Server-Sent Events: "snapshot" once, then "quote" per update
 * </pre>
 */
@WebServlet(urlPatterns = "/api/market/*", asyncSupported = true)
public final class MarketServlet extends ApiServlet {
    @EJB
    private MarketControl market;
    @EJB
    private MarketStreamHub marketStream;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<String> path = segments(request);
        if (path.size() == 1 && "stream".equals(path.get(0))) {
            marketStream.open(request, response);
            return;
        }
        respond(request, response, () -> {
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
