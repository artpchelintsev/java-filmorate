package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Repository
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY user_id ASC";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public User createUser(User user) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("user_id");

        Map<String, Object> values = new HashMap<>();
        values.put("email", user.getEmail());
        values.put("login", user.getLogin());
        values.put("name", user.getName());
        values.put("birthday", user.getBirthday());

        Number id = simpleJdbcInsert.executeAndReturnKey(values);
        user.setId(id.longValue());

        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ?", user.getId());
        if (user.getFriends() != null) {
            for (Long friendId : user.getFriends()) {
                addFriend(user.getId(), friendId);
            }
        }

        return user;
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден.");
        }
    }

    @Override
    public void deleteUser(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());

        user.setFriends(new HashSet<>());

        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);

        String checkSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        boolean exists = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId) > 0;

        if (exists) {
            jdbcTemplate.update(
                    "UPDATE friends SET confirm_status = false WHERE user_id = ? AND friend_id = ?",
                    userId, friendId
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO friends (user_id, friend_id, confirm_status) VALUES (?, ?, false)",
                    userId, friendId
            );
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM friends WHERE user_id = ? AND friend_id = ? AND confirm_status = false",
                userId, friendId
        );

        if (deleted == 0) {
            jdbcTemplate.update(
                    "UPDATE friends SET confirm_status = false WHERE user_id = ? AND friend_id = ?",
                    userId, friendId
            );
        }
    }

    @Override
    public Collection<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM friends f " +
                "JOIN users u ON f.friend_id = u.user_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.* FROM friends f1 " +
                "JOIN friends f2 ON f1.friend_id = f2.friend_id " +
                "JOIN users u ON f1.friend_id = u.user_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }

    public void confirmFriend(Long userId, Long friendId) {
        jdbcTemplate.update(
                "UPDATE friends SET confirm_status = true WHERE user_id = ? AND friend_id = ?",
                userId, friendId
        );
        jdbcTemplate.update(
                "UPDATE friends SET confirm_status = true WHERE user_id = ? AND friend_id = ?",
                friendId, userId
        );

        String checkReverseSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        int reverseCount = jdbcTemplate.queryForObject(checkReverseSql, Integer.class, friendId, userId);

        if (reverseCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO friends (user_id, friend_id, confirm_status) VALUES (?, ?, true)",
                    friendId, userId
            );
        }
    }
}
