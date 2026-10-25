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

@WebServlet("/auth/login")
public final class LoginServlet extends HttpServlet {
    private final ObjectMapper json = new ObjectMapper();
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonNode body = json.readTree(request.getInputStream());
            var user = ApplicationContext.auth().authenticate(text(body, "username"), text(body, "password"));
            var old = request.getSession(false);
            if (old != null) old.invalidate();
            var session = request.getSession(true);
            session.setAttribute("authenticatedUserId", user.userId());
            session.setAttribute("authenticatedUsername", user.username());
            response.setContentType("application/json");
            json.writeValue(response.getWriter(), Map.of("userId", user.userId(), "username", user.username()));
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
        }
    }
    private String text(JsonNode body, String name) {
        JsonNode value = body == null ? null : body.get(name);
        if (value == null || value.asText().isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.asText();
    }
}
