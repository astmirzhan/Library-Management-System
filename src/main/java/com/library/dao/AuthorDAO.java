package com.library.dao;

import com.library.model.Author;

import java.sql.SQLException;
import java.util.List;

/** DAO contract for the AUTHOR table. */
public interface AuthorDAO extends BaseDAO<Author, Integer> {
    List<Author> findByBookId(int bookId) throws SQLException;
}
