package com.example.asset.boundary.http;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;

import com.example.asset.application.ApplicationContext;
import com.example.asset.control.NotFoundException;
import com.example.asset.control.UnavailableException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Base class for JSON endpoints: maps control exceptions to HTTP status codes in one place. */
public abstract class ApiServlet extends HttpServlet {
    private static final System.Logger LOG = System.getLogger(ApiServlet.class.getName());

    @FunctionalInterface
    protected interface Action {
        void run() throws IOException;
    }

    protected final ApplicationContext app() {
        return ApplicationContext.from(getServletContext());
    }

    protected final void respond(HttpServletRequest request, HttpServletResponse response, Action action)
            throws IOException {
        try {
            action.run();
        } catch (UnauthorizedException exception) {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } catch (NotFoundException exception) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
        } catch (SecurityException exception) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (UnavailableException exception) {
            LOG.log(Level.WARNING, request.getMethod() + " " + request.getRequestURI() + " failed", exception);
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            LOG.log(Level.ERROR, request.getMethod() + " " + request.getRequestURI() + " failed", exception);
            if (!response.isCommitted()) {
                Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Something went wrong. Please try again.");
            }
        }
    }

    /** Path segments after the servlet mapping, e.g. {@code /api/portfolios/7/trades} gives [7, trades]. */
    protected static List<String> segments(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return List.of();
        }
        return Arrays.stream(pathInfo.split("/")).filter(segment -> !segment.isEmpty()).toList();
    }

    protected static long id(String segment) {
        try {
            long value = Long.parseLong(segment);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Reported below.
        }
        throw new NotFoundException("Not found");
    }

    protected static void notFound(HttpServletResponse response) throws IOException {
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    protected static void methodNotAllowed(HttpServletResponse response, String allow, String message)
            throws IOException {
        response.setHeader("Allow", allow);
        Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
    }
}
