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
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));

        List<Movie> movies = gson.fromJson(response.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_afterAddingMovies_returnsMoviesList() throws Exception {
        Movie movie1 = new Movie(0, "Inception", "Christopher Nolan", 2010);
        Movie movie2 = new Movie(0, "The Matrix", "Lana Wachowski", 1999);

        postMovie(movie1);
        postMovie(movie2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(response.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(2, movies.size());

        Movie first = movies.get(0);
        assertEquals("Inception", first.getTitle());
        assertEquals("Christopher Nolan", first.getDirector());
        assertEquals(2010, first.getYear());

        Movie second = movies.get(1);
        assertEquals("The Matrix", second.getTitle());
        assertEquals("Lana Wachowski", second.getDirector());
        assertEquals(1999, second.getYear());
    }

    @Test
    void postMovie_withValidData_returns201AndCreatedMovie() throws Exception {
        Movie newMovie = new Movie(0, "Interstellar", "Christopher Nolan", 2014);
        String json = gson.toJson(newMovie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));

        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.matches("/movies/\\d+"));

        Movie created = gson.fromJson(response.body(), Movie.class);
        assertNotEquals(0, created.getId());
        assertEquals("Interstellar", created.getTitle());
        assertEquals("Christopher Nolan", created.getDirector());
        assertEquals(2014, created.getYear());
    }

    @Test
    void postMovie_withEmptyTitle_returns400() throws Exception {
        Movie invalidMovie = new Movie(0, "", "Some Director", 2020);
        String json = gson.toJson(invalidMovie);

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
    void postMovie_withInvalidJson_returns400() throws Exception {
        String invalidJson = "{ title: \"Inception\", \"director\": \"Christopher Nolan\", \"year\": 2010 }";

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
    void getUnknownEndpoint_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/unknown"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Endpoint not found"));
    }

    private void postMovie(Movie movie) throws Exception {
        String json = gson.toJson(movie);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "Не удалось добавить фильм: " + response.body());
    }
}