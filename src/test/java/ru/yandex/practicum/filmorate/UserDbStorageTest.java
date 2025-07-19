package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserDbStorage.class})
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("user@example.com");
        testUser.setLogin("login");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void testCreateUser() {
        User createdUser = userStorage.createUser(testUser);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void testUpdateUser() {
        User createdUser = userStorage.createUser(testUser);
        createdUser.setName("Updated Name");

        User updatedUser = userStorage.updateUser(createdUser);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    void testGetUserById() {
        User createdUser = userStorage.createUser(testUser);
        Optional<User> foundUser = Optional.ofNullable(userStorage.getUserById(createdUser.getId()));

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user).hasFieldOrPropertyWithValue("email", "user@example.com")
                );
    }

    @Test
    void testAddAndGetFriends() {
        User user1 = userStorage.createUser(testUser);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.createUser(user2);

        // Добавляем друга (заявка от user1 к user2)
        userStorage.addFriend(user1.getId(), user2.getId());

        // Для получения друзей нужно учесть, что getFriends возвращает тех,
        // к кому user1 отправил заявку, но не обязательно подтвержденных друзей
        Collection<User> friends = userStorage.getFriends(user1.getId());

        assertThat(friends).hasSize(1);
        assertThat(friends.iterator().next().getId()).isEqualTo(user2.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = userStorage.createUser(testUser);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.createUser(user2);

        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.removeFriend(user1.getId(), user2.getId());

        Collection<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    void testGetCommonFriends() {
        User user1 = userStorage.createUser(testUser);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.createUser(user2);

        User commonFriend = new User();
        commonFriend.setEmail("friend@example.com");
        commonFriend.setLogin("friend");
        commonFriend.setName("Common Friend");
        commonFriend.setBirthday(LocalDate.of(1990, 10, 10));
        commonFriend = userStorage.createUser(commonFriend);

        // Добавляем заявки на дружбу от обоих пользователей к общему другу
        userStorage.addFriend(user1.getId(), commonFriend.getId());
        userStorage.addFriend(user2.getId(), commonFriend.getId());

        // Получаем общих друзей (тех, к кому оба отправили заявки)
        Collection<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.iterator().next().getId()).isEqualTo(commonFriend.getId());
    }
}
