package com.example.asset.application;

import java.lang.System.Logger.Level;
import java.util.EnumSet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.annotation.WebListener;

@WebListener
public final class ApplicationLifecycle implements ServletContextListener {
    private static final System.Logger LOG = System.getLogger(ApplicationLifecycle.class.getName());
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private ApplicationContext application;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        configureSessions(servletContext);
        try {
            application = ApplicationContext.start(AppConfig.fromEnvironment(), servletContext);
            LOG.log(Level.INFO, "Asset management service started");
        } catch (RuntimeException exception) {
            LOG.log(Level.ERROR, "Asset management service failed to start: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (application != null) {
            application.close();
        }
    }

    private static void configureSessions(ServletContext servletContext) {
        try {
            servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
            servletContext.setSessionTimeout(SESSION_TIMEOUT_MINUTES);
            SessionCookieConfig cookie = servletContext.getSessionCookieConfig();
            cookie.setHttpOnly(true);
            cookie.setAttribute("SameSite", "Strict");
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Could not apply session cookie hardening", exception);
        }
    }
}
