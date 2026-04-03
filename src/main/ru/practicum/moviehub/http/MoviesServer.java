package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler(store));
            server.createContext("/movies/", new MovieHandler(store));
            server.setExecutor(null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server on port " + port, e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Movies server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("Movies server stopped");
    }
}