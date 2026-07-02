package com.library.dao;

import com.library.model.Review;

import java.sql.SQLException;
import java.util.List;

/** DAO contract for the REVIEW table. */
public interface ReviewDAO extends BaseDAO<Review, Integer> {
    List<Review> findByBookId(int bookId) throws SQLException;
    List<Review> findByUserId(int userId) throws SQLException;
    double getAverageRating(int bookId) throws SQLException;
    boolean existsByUserAndBook(int userId, int bookId) throws SQLException;
    int count() throws SQLException;
}
