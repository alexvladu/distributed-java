package com.example.asset.boundary;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Serves the HTML pages; the JSPs live under WEB-INF so they are reachable only through this servlet. */
@WebServlet(urlPatterns = {"", "/live-market", "/assets", "/portfolios"})
public final class PageServlet extends HttpServlet {
    private static final Map<String, String> VIEWS = Map.of(
            "", "home",
            "/live-market", "live-market",
            "/assets", "assets",
            "/portfolios", "portfolios");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String view = VIEWS.get(request.getServletPath());
        if (view == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }
}
