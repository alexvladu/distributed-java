package com.example.asset.boundary.auth;

import java.io.IOException;

import com.example.asset.boundary.http.SessionSupport;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/auth/logout")
public final class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionSupport.logout(request);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
