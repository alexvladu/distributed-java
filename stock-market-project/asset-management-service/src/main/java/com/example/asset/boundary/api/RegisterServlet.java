package com.example.asset.boundary.api;

import java.io.IOException;
import java.util.Map;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;
import com.example.asset.control.AuthControl;
import com.example.asset.entity.user.User;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/auth/register")
public final class RegisterServlet extends ApiServlet {
    public record RegisterRequest(String username, String email, String password) {
    }

    @EJB
    private AuthControl auth;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            RegisterRequest body = Json.read(request, RegisterRequest.class);
            User user = auth.register(body.username(), body.email(), body.password());
            SessionSupport.login(request, user);
            Json.write(response, HttpServletResponse.SC_CREATED,
                    Map.of("userId", user.userId(), "username", user.username()));
        });
    }
}
