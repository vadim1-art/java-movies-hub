package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (!path.equals("/movies")) {
            sendNotFound(exchange, "Not found");
            return;
        }

        if ("GET".equals(method)) {
            sendJson(exchange, store.getAllMovies(), 200);
        } else if ("POST".equals(method)) {
            handlePost(exchange);
        } else {
            sendNotFound(exchange, "Method not allowed");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(body, Movie.class);

            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                sendBadRequest(exchange, "Movie title is required");
                return;
            }

            Movie created = store.addMovie(movie);
            exchange.getResponseHeaders().set("Location", "/movies/" + created.getId());
            sendJson(exchange, created, 201);
        } catch (JsonSyntaxException e) {
            sendBadRequest(exchange, "Invalid JSON format");
        }
    }
}