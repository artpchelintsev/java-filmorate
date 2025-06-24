package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(),
                "Корректный фильм должен проходить валидацию.");
    }

    @Test
    void shouldNotValidateUserWithEmptyEmail() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(),
                "Пользователь с пустым email не должен проходить валидацию");

        assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("email")),
                "Ожидалась ошибка валидации для поля email");
    }

    @Test
    void shouldNotValidateUserWithFutureBirthday() {
        User user = new User();
        user.setEmail("email@example.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(),
                "Пользователь с датой рождения в будущем не должен проходить валидацию");

        assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("birthday")),
                "Ожидалась ошибка валидации для поля birthday");
    }
}
