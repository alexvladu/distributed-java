package ro.sistemedistribuite;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String name = request.getParameter("name");
        if (name == null || name.isBlank()) {
            name = "lume";
        }

        String safeName = escapeHtml(name.trim());
        response.getWriter().printf(
                """
                <!DOCTYPE html>
                <html lang="ro">
                <head>
                    <meta charset="UTF-8">
                    <title>Hello Servlet</title>
                </head>
                <body>
                    <h1>Salut, %s!</h1>
                    <p>Servletul Java funcționează.</p>
                    <p>Ora UTC: %s</p>
                </body>
                </html>
                """,
                safeName,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
