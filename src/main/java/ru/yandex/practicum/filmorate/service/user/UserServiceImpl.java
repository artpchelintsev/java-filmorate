package ru.yandex.practicum.filmorate.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserServiceImpl(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Override
    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    @Override
    public User createUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.createUser(user);
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null || userStorage.getUserById(user.getId()) == null) {
            throw new NotFoundException("Пользователь с таким id не существует.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.updateUser(user);
    }

    @Override
    public User getUserById(Long id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден.");
        }
        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        userStorage.addFriend(userId, friendId);
    }

    @Override
    public void confirmFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);

        userStorage.confirmFriend(userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);

        userStorage.removeFriend(userId, friendId);
    }

    @Override
    public Collection<User> getFriends(Long userId) {
        getUserById(userId);
        return userStorage.getFriends(userId);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
            getUserById(userId);
            getUserById(otherId);
            return userStorage.getCommonFriends(userId, otherId);
        }
}
