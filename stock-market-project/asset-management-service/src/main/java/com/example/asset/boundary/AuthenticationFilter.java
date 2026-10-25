package com.example.asset.boundary;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public final class AuthenticationFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        var session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("authenticatedUserId") == null) {
            ((HttpServletResponse) response).sendRedirect(
                    httpRequest.getContextPath() + "/auth.html");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.equals("/auth.html")
                || path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.equals("/auth/logout")
                || path.equals("/favicon.ico");
    }
}
