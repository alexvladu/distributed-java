package com.example.asset.boundary.api;

import java.io.IOException;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The signed-in user, used by the page header. */
@WebServlet("/api/session")
public final class SessionServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> Json.write(response, HttpServletResponse.SC_OK,
                app().users().get(SessionSupport.requireUserId(request))));
    }
}
