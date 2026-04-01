package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange exchange, Object data, int statusCode) throws IOException {
        String response = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendError(HttpExchange exchange, String message, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, message.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendNotFound(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, message, 404);
    }

    protected void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, message, 400);
    }

    protected void sendInternalError(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, message, 500);
    }
}