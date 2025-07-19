package ru.yandex.practicum.filmorate.service.mpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.storage.dao.MpaDao;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

@Service
public class MpaServiceImpl implements MpaService {
    private final MpaDao mpaDao;

    @Autowired
    public MpaServiceImpl(MpaDao mpaDao) {
        this.mpaDao = mpaDao;
    }

    @Override
    public Collection<MpaRating> getAllMpaRatings() {
        return mpaDao.getAllMpaRatings();
    }

    @Override
    public MpaRating getMpaRatingById(int id) {
        return mpaDao.getMpaRatingById(id);
    }
}
