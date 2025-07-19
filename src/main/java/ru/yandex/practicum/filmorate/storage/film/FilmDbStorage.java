package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT * FROM films ORDER BY film_id ASC";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        return films;
    }

    @Override
    public Film createFilm(Film film) {
        if (film.getMpa() != null) {
            int mpaId = film.getMpa().getId();
            if (mpaId <= 0) {
                throw new ValidationException("MPA ID должен быть положительным");
            }

            try {
                jdbcTemplate.queryForObject(
                        "SELECT * FROM rating WHERE rating_id = ?",
                        this::mapRowToMpa,
                        mpaId
                );
            } catch (EmptyResultDataAccessException e) {
                throw new RuntimeException("Ошибка сервера: MPA с id=" + mpaId + " не найден"); // 500
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                int genreId = genre.getId();

                if (genreId <= 0) {
                    throw new ValidationException("ID жанра должно быть положительным числом");
                }

                try {
                    jdbcTemplate.queryForObject(
                            "SELECT * FROM genres WHERE genre_id = ?",
                            this::mapRowToGenre,
                            genreId
                    );
                } catch (EmptyResultDataAccessException e) {
                    throw new NotFoundException("Жанр с id=" + genreId + " не найден");
                }
            }
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM films", Long.class);
        if (count == 0) {
            String sql = "INSERT INTO films (film_id, name, description, release_date, duration, rating_id) " +
                    "VALUES (1, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql,
                    film.getName(),
                    film.getDescription(),
                    film.getReleaseDate(),
                    film.getDuration(),
                    film.getMpa() != null ? film.getMpa().getId() : null);
            film.setId(1L);
        } else {
            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName("films")
                    .usingGeneratedKeyColumns("film_id");

            Map<String, Object> values = new HashMap<>();
            values.put("name", film.getName());
            values.put("description", film.getDescription());
            values.put("release_date", film.getReleaseDate());
            values.put("duration", film.getDuration());
            values.put("rating_id", film.getMpa() != null ? film.getMpa().getId() : null);

            Number id = simpleJdbcInsert.executeAndReturnKey(values);
            film.setId(id.longValue());
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                addGenreToFilm(film.getId(), genre.getId());
            }
        }

        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rating_id = ? WHERE film_id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        jdbcTemplate.update("DELETE FROM films_genres WHERE film_id = ?", film.getId());

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                addGenreToFilm(film.getId(), genre.getId());
            }
        }

        return film;
    }

    @Override
    public Film getFilmById(Long id) {
        String sql = "SELECT * FROM films WHERE film_id = ?";
        return jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
    }

    @Override
    public void deleteFilm(Long id) {
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "MERGE INTO films_likes (film_id, user_id) VALUES (?, ?) ";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM films_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public Collection<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, COUNT(fl.user_id) AS likes_count FROM films f " +
                "LEFT JOIN films_likes fl ON f.film_id = fl.film_id " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRowToFilm, count);
    }


    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int ratingId = rs.getInt("rating_id");
        if (!rs.wasNull()) {
            try {
                String mpaSql = "SELECT * FROM rating WHERE rating_id = ?";
                MpaRating mpa = jdbcTemplate.queryForObject(mpaSql, this::mapRowToMpa, ratingId);
                film.setMpa(mpa);
            } catch (EmptyResultDataAccessException e) {
                film.setMpa(null);
            }
        }

        String genresSql = "SELECT g.genre_id, g.genre_name FROM films_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ?";
        Set<Genre> genres = new HashSet<>(jdbcTemplate.query(genresSql, this::mapRowToGenre, film.getId()));
        film.setGenres(genres);

        String likesSql = "SELECT user_id FROM films_likes WHERE film_id = ?";
        Set<Long> likes = new HashSet<>(jdbcTemplate.query(
                likesSql,
                (rsLikes, rowNumLikes) -> rsLikes.getLong("user_id"),
                film.getId()));
        film.setLikes(likes);

        return film;
    }

    private MpaRating mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getInt("rating_id"));
        mpa.setName(rs.getString("name_mpa"));
        return mpa;
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("genre_id"));
        genre.setName(rs.getString("genre_name"));
        return genre;
    }

    private void addGenreToFilm(Long filmId, Integer genreId) {
        String sql = "INSERT INTO films_genres (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, genreId);
    }

    private boolean mpaExists(int mpaId) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT * FROM rating WHERE rating_id = ?",
                    this::mapRowToMpa,
                    mpaId
            );
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}
