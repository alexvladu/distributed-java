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

@WebServlet("/auth/login")
public final class LoginServlet extends ApiServlet {
    public record LoginRequest(String username, String password) {
    }

    @EJB
    private AuthControl auth;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            LoginRequest body = Json.read(request, LoginRequest.class);
            User user;
            try {
                user = auth.authenticate(body.username(), body.password());
            } catch (SecurityException exception) {
                Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                return;
            }
            SessionSupport.login(request, user);
            Json.write(response, HttpServletResponse.SC_OK,
                    Map.of("userId", user.userId(), "username", user.username()));
        });
    }
}
