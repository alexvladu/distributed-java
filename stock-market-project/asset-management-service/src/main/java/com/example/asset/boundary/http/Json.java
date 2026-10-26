package com.example.asset.boundary.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The single JSON configuration used by every HTTP boundary. */
public final class Json {
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    private Json() {
    }

    public static <T> T read(HttpServletRequest request, Class<T> type) {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new IllegalArgumentException("Request body is too large");
        }
        try (InputStream input = request.getInputStream()) {
            byte[] body = input.readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                throw new IllegalArgumentException("Request body is too large");
            }
            if (body.length == 0) {
                throw new IllegalArgumentException("Request body is required");
            }
            T value = MAPPER.readValue(body, type);
            if (value == null) {
                throw new IllegalArgumentException("Request body is required");
            }
            return value;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Request body is not valid JSON");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void write(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        MAPPER.writeValue(response.getWriter(), value);
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, Map.of("error", message == null ? "Request failed" : message));
    }

    public static String stringify(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize JSON", exception);
        }
    }
}
