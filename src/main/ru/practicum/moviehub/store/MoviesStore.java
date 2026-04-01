package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public Movie addMovie(Movie movie) {
        int newId = idGenerator.getAndIncrement();
        movie.setId(newId);
        movies.put(newId, movie);
        return movie;
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> getMovieById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idGenerator.set(1);
    }
}