package com.example.asset.boundary.api;

import java.io.IOException;
import java.util.List;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;
import com.example.asset.control.NotFoundException;
import com.example.asset.entity.user.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Profile of the signed-in user (PostgreSQL). Registration happens at /auth/register. */
@WebServlet("/api/users/*")
public final class UserServlet extends ApiServlet {
    public record ProfileRequest(String username, String email) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            List<String> path = segments(request);
            if (path.size() > 1 || (path.size() == 1 && id(path.get(0)) != userId)) {
                throw new NotFoundException("User not found");
            }
            Json.write(response, HttpServletResponse.SC_OK, app().users().get(userId));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response, "GET, PUT, DELETE", "Register new users at /auth/register");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            long id = requireSingleId(request);
            ProfileRequest body = Json.read(request, ProfileRequest.class);
            User updated = app().users().update(userId, id, body.username(), body.email());
            SessionSupport.updateUsername(request, updated.username());
            Json.write(response, HttpServletResponse.SC_OK, updated);
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> {
            long userId = SessionSupport.requireUserId(request);
            app().users().delete(userId, requireSingleId(request));
            SessionSupport.logout(request);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        });
    }

    private static long requireSingleId(HttpServletRequest request) {
        List<String> path = segments(request);
        if (path.size() != 1) {
            throw new NotFoundException("User not found");
        }
        return id(path.get(0));
    }
}
