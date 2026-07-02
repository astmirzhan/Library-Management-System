package com.library.dao;

import com.library.model.Genre;

import java.sql.SQLException;
import java.util.List;

/** DAO contract for the GENRE table. */
public interface GenreDAO extends BaseDAO<Genre, Integer> {
    List<Genre> findByBookId(int bookId) throws SQLException;
}
