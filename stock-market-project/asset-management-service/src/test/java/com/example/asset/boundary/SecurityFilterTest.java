package com.example.asset.boundary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class SecurityFilterTest {
    private static HttpServletRequest request(Map<String, String> headers) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getHeader" -> headers.get((String) arguments[0]);
                    case "getScheme" -> "http";
                    case "getServerName" -> "localhost";
                    case "getServerPort" -> 8081;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void acceptsSameOriginScriptRequests() {
        assertTrue(SecurityFilter.isSameOriginScriptRequest(request(Map.of(
                "X-Requested-With", "XMLHttpRequest",
                "Origin", "http://localhost:8081"))));
        assertTrue(SecurityFilter.isSameOriginScriptRequest(request(Map.of(
                "X-Requested-With", "XMLHttpRequest"))));
    }

    @Test
    void rejectsRequestsWithoutTheHeaderOrFromAnotherOrigin() {
        assertFalse(SecurityFilter.isSameOriginScriptRequest(request(Map.of())));
        assertFalse(SecurityFilter.isSameOriginScriptRequest(request(Map.of(
                "Origin", "http://localhost:8081"))));
        assertFalse(SecurityFilter.isSameOriginScriptRequest(request(Map.of(
                "X-Requested-With", "XMLHttpRequest",
                "Origin", "https://evil.example"))));
        assertFalse(SecurityFilter.isSameOriginScriptRequest(request(Map.of(
                "X-Requested-With", "XMLHttpRequest",
                "Origin", "null"))));
    }
}
