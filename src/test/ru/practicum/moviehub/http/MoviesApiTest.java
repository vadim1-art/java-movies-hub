package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static MoviesServer server;
    private static MoviesStore store;
    private static final int PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    @BeforeAll
    static void startServer() {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(response.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_afterAdding_returnsList() throws Exception {
        postMovie(new Movie(0, "A", "B", 2000));
        postMovie(new Movie(0, "C", "D", 2001));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(response.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(2, movies.size());
    }

    @Test
    void postMovie_valid_returns201() throws Exception {
        Movie newMovie = new Movie(0, "Inception", "Nolan", 2010);
        String json = gson.toJson(newMovie);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElse("").matches("/movies/\\d+"));
        Movie created = gson.fromJson(response.body(), Movie.class);
        assertNotEquals(0, created.getId());
    }

    @Test
    void postMovie_emptyTitle_returns400() throws Exception {
        Movie invalid = new Movie(0, "", "Nolan", 2010);
        String json = gson.toJson(invalid);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("title is required"));
    }

    @Test
    void postMovie_invalidJson_returns400() throws Exception {
        String invalidJson = "{ \"title\": \"Inception\", \"year\": 2010, }";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Invalid JSON format"));
    }

    @Test
    void getMovieById_existing_returnsMovie() throws Exception {
        Movie added = postMovieAndGet(new Movie(0, "Test", "Director", 2020));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Movie got = gson.fromJson(response.body(), Movie.class);
        assertEquals(added.getId(), got.getId());
    }

    @Test
    void getMovieById_notFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Movie not found"));
    }

    @Test
    void getMovieById_invalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Invalid movie id format"));
    }

    @Test
    void deleteMovie_existing_returns204() throws Exception {
        Movie added = postMovieAndGet(new Movie(0, "ToDelete", "Del", 2022));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, response.statusCode());
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMovie_notFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Movie not found"));
    }

    @Test
    void putMovie_existing_returns200() throws Exception {
        Movie original = postMovieAndGet(new Movie(0, "Old", "OldDir", 2000));
        Movie updated = new Movie(original.getId(), "New Title", "NewDir", 2023);
        String json = gson.toJson(updated);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + original.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Movie got = gson.fromJson(response.body(), Movie.class);
        assertEquals("New Title", got.getTitle());
    }

    @Test
    void putMovie_notFound_returns404() throws Exception {
        Movie updated = new Movie(999, "Ghost", "GhostDir", 2023);
        String json = gson.toJson(updated);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Movie not found"));
    }

    @Test
    void putMovie_emptyTitle_returns400() throws Exception {
        Movie original = postMovieAndGet(new Movie(0, "Valid", "Dir", 2020));
        Movie invalid = new Movie(original.getId(), "", "Dir", 2020);
        String json = gson.toJson(invalid);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + original.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("title is required"));
    }

    private void postMovie(Movie movie) throws Exception {
        String json = gson.toJson(movie);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    private Movie postMovieAndGet(Movie movie) throws Exception {
        String json = gson.toJson(movie);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        return gson.fromJson(response.body(), Movie.class);
    }
}