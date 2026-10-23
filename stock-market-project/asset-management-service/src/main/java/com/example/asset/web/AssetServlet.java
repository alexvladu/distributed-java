package com.example.asset.web;

import java.io.IOException;

import com.example.asset.service.AssetService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/assets")
public class AssetServlet extends HttpServlet {
    private final AssetService assetService = new AssetService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("assets", assetService.findAll());
            request.getRequestDispatcher("/WEB-INF/views/assets.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            throw new ServletException("Unable to load asset data", exception);
        }
    }
}
