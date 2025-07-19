package ru.yandex.practicum.filmorate.storage.dao;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface MpaDao {
    Collection<MpaRating> getAllMpaRatings();

    MpaRating getMpaRatingById(int id);
}
