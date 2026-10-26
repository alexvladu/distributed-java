package com.example.asset.boundary;

import java.io.IOException;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.control.HealthControl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/health")
public final class HealthServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            HealthControl.Health health = app().health().check();
            Json.write(response, "UP".equals(health.status())
                    ? HttpServletResponse.SC_OK
                    : HttpServletResponse.SC_SERVICE_UNAVAILABLE, health);
        });
    }
}
