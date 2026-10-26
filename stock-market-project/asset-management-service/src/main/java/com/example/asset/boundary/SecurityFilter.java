package com.example.asset.boundary;

import java.io.IOException;
import java.util.Set;

import com.example.asset.boundary.http.Json;
import com.example.asset.boundary.http.SessionSupport;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * One filter (so ordering is deterministic) for security headers, CSRF protection, and authentication.
 * Must support async because the market stream starts an async request behind it.
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public final class SecurityFilter implements Filter {
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";
    static final String CSRF_HEADER = "X-Requested-With";
    static final String CSRF_HEADER_VALUE = "XMLHttpRequest";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth.html", "/auth/login", "/auth/register", "/auth/logout", "/health", "/favicon.ico");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        addSecurityHeaders(response);

        if (!SAFE_METHODS.contains(request.getMethod()) && !isSameOriginScriptRequest(request)) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN, "Cross-site request blocked");
            return;
        }

        String path = path(request);
        if (isPublic(path) || SessionSupport.userId(request).isPresent()) {
            chain.doFilter(request, response);
            return;
        }
        if (expectsJson(request, path)) {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        } else {
            response.sendRedirect(request.getContextPath() + "/auth.html");
        }
    }

    private static void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    }

    /**
     * Browsers only attach a custom header to same-origin requests (cross-origin ones need a CORS preflight
     * this app never grants), and the Origin header, when present, must match this server.
     */
    static boolean isSameOriginScriptRequest(HttpServletRequest request) {
        if (!CSRF_HEADER_VALUE.equals(request.getHeader(CSRF_HEADER))) {
            return false;
        }
        String origin = request.getHeader("Origin");
        return origin == null || origin.equalsIgnoreCase(expectedOrigin(request));
    }

    private static String expectedOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port);
    }

    /** Decoded, normalized path within the application (never the raw URI). */
    private static String path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path) || path.startsWith("/static/");
    }

    private static boolean expectsJson(HttpServletRequest request, String path) {
        String accept = request.getHeader("Accept");
        return path.startsWith("/api/")
                || "/dashboard".equals(path)
                || request.getHeader(CSRF_HEADER) != null
                || (accept != null && (accept.contains("application/json") || accept.contains("text/event-stream")));
    }
}
