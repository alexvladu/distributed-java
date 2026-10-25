package com.example.asset.boundary.auth;

import com.example.asset.application.ApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/auth/register")
public final class RegisterServlet extends HttpServlet {
    private final ObjectMapper json = new ObjectMapper();
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode body = json.readTree(request.getInputStream());
            var user = ApplicationContext.auth().register(text(body, "username"), text(body, "email"), text(body, "password"));
            request.getSession(true).setAttribute("authenticatedUserId", user.userId());
            request.getSession().setAttribute("authenticatedUsername", user.username());
            response.setStatus(HttpServletResponse.SC_CREATED);
            json.writeValue(response.getWriter(), Map.of("userId", user.userId(), "username", user.username()));
        } catch (RuntimeException exception) {
            response.sendError(exception instanceof SecurityException ? 403 : 400, exception.getMessage());
        }
    }
    private String text(JsonNode body, String name) {
        JsonNode value = body == null ? null : body.get(name);
        if (value == null || value.asText().isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.asText();
    }
}
