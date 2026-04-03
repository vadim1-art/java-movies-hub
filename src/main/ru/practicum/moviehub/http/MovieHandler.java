package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MovieHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MovieHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] parts = path.split("/");
        if (parts.length != 3) {
            sendNotFound(exchange, "Invalid path");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            sendBadRequest(exchange, "Invalid movie id format");
            return;
        }

        switch (method) {
            case "GET":
                handleGetById(exchange, id);
                break;
            case "DELETE":
                handleDelete(exchange, id);
                break;
            case "PUT":
                handlePut(exchange, id);
                break;
            default:
                sendNotFound(exchange, "Method not allowed");
        }
    }

    private void handleGetById(HttpExchange exchange, int id) throws IOException {
        Optional<Movie> movieOpt = store.getMovieById(id);
        if (movieOpt.isEmpty()) {
            sendNotFound(exchange, "Movie not found");
        } else {
            sendJson(exchange, movieOpt.get(), 200);
        }
    }

    private void handleDelete(HttpExchange exchange, int id) throws IOException {
        boolean deleted = store.deleteMovie(id);
        if (deleted) {
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
        } else {
            sendNotFound(exchange, "Movie not found");
        }
    }

    private void handlePut(HttpExchange exchange, int id) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Movie updated = gson.fromJson(body, Movie.class);

            if (updated.getTitle() == null || updated.getTitle().isBlank()) {
                sendBadRequest(exchange, "Movie title is required");
                return;
            }

            if (store.getMovieById(id).isEmpty()) {
                sendNotFound(exchange, "Movie not found");
                return;
            }

            updated.setId(id);
            store.updateMovie(updated);
            sendJson(exchange, updated, 200);
        } catch (JsonSyntaxException e) {
            sendBadRequest(exchange, "Invalid JSON format");
        }
    }
}