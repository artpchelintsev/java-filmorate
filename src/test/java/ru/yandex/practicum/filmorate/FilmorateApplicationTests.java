package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldCreateValidFilm() {
        Film film = new Film();
        film.setName("Correct Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertTrue(validator.validate(film).isEmpty(),
                "Корректный фильм проходит валидацию.");
    }

    @Test
    void shouldNotValidateUserWithEmptyEmail() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertFalse(validator.validate(user).isEmpty(),
                "Пользователь с пустым email не проходит валидацию.");
    }

    @Test
    void shouldNotValidateFilmWithEarlyReleaseDate() {
        Film film = new Film();
        film.setName("Early Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(120);

        FilmController filmController = new FilmController();
        assertThrows(ValidationException.class, () -> filmController.createFilm(film),
                "Фильм с датой релиза раньше 28.12.1895 должен вызывать ValidationException");
    }

    @Test
    void shouldNotValidateUserWithFutureBirthday() {
        User user = new User();
        user.setEmail("email@example.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertFalse(validator.validate(user).isEmpty(),
                "Пользователь с датой рождения в будущем не проходит валидацию.");
    }

}
