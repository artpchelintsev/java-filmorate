package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FilmDbStorage.class})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    private Film testFilm;

    @BeforeEach
    void setUp() {
        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test Description");
        testFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        testFilm.setMpa(mpa);
    }

    @Test
    void testCreateFilm() {
        Film createdFilm = filmStorage.createFilm(testFilm);

        assertThat(createdFilm).isNotNull();
        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getName()).isEqualTo(testFilm.getName());
    }

    @Test
    void testUpdateFilm() {
        Film createdFilm = filmStorage.createFilm(testFilm);
        createdFilm.setName("Updated Film Name");

        Film updatedFilm = filmStorage.updateFilm(createdFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film Name");
    }

    @Test
    void testGetFilmById() {
        Film createdFilm = filmStorage.createFilm(testFilm);
        Optional<Film> foundFilm = Optional.ofNullable(filmStorage.getFilmById(createdFilm.getId()));

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(film ->
                        assertThat(film).hasFieldOrPropertyWithValue("name", "Test Film")
                );
    }

    @Test
    void testGetAllFilms() {
        filmStorage.createFilm(testFilm);
        Collection<Film> films = filmStorage.getAllFilms();

        assertThat(films).isNotEmpty();
        assertThat(films).hasSize(1);
    }

}
