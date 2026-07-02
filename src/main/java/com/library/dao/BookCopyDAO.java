package com.library.dao;

import com.library.model.BookCopy;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** DAO contract for the BOOK_COPY table. */
public interface BookCopyDAO extends BaseDAO<BookCopy, Integer> {
    List<BookCopy> findByBookId(int bookId) throws SQLException;
    Optional<BookCopy> findAvailableCopy(int bookId) throws SQLException;
    int countAll() throws SQLException;
    int countByCondition(BookCopy.Condition condition) throws SQLException;
}
