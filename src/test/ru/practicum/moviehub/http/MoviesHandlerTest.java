package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoviesHandlerTest {
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
    void shouldReturnEmptyArrayWhenNoMovies() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));

        List<Movie> movies = gson.fromJson(response.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
    }

    @Test
    void shouldReturnMoviesListAfterAdding() throws IOException, InterruptedException {
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
    void shouldAddMovieAndReturn201() throws IOException, InterruptedException {
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
        assertTrue(location.matches("/movies/\\d+"), "Location должен содержать id созданного фильма");

        Movie created = gson.fromJson(response.body(), Movie.class);
        assertNotEquals(0, created.getId(), "id должен быть сгенерирован");
        assertEquals("Interstellar", created.getTitle());
        assertEquals("Christopher Nolan", created.getDirector());
        assertEquals(2014, created.getYear());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        List<Movie> allMovies = gson.fromJson(getResponse.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(1, allMovies.size());
        assertEquals(created.getId(), allMovies.get(0).getId());
    }

    @Test
    void shouldReturn400WhenTitleMissing() throws IOException, InterruptedException {
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
    void shouldReturn400WhenInvalidJson() throws IOException, InterruptedException {
        String invalidJson = "{ \"title\": \"Inception\", \"director\": \"Christopher Nolan\", year: 2010 }"; // пропущены кавычки у year

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
    void shouldReturn404ForUnknownEndpoint() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/unknown"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Endpoint not found"));
    }

    private void postMovie(Movie movie) throws IOException, InterruptedException {
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