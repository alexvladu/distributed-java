package com.example.asset.boundary.api;

import java.io.IOException;

import com.example.asset.boundary.http.ApiServlet;
import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;
import com.example.asset.control.DashboardControl;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The signed-in user's data joined across PostgreSQL, SQL Server, and Oracle. */
@WebServlet("/dashboard")
public final class DashboardServlet extends ApiServlet {
    @EJB
    private DashboardControl dashboard;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, () -> Json.write(response, HttpServletResponse.SC_OK,
                dashboard.load(SessionSupport.requireUserId(request))));
    }
}
