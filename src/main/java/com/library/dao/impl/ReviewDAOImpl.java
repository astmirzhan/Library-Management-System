package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.Review;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the REVIEW table.
 * Provides CRUD operations for book reviews.
 */
@Repository
public class ReviewDAOImpl implements ReviewDAO {

    private static final Logger logger = LogManager.getLogger(ReviewDAOImpl.class);
    private final DatabaseConnection dbConnection;

    public ReviewDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public Review save(Review review) throws SQLException {
        String sql = "INSERT INTO review (user_id, book_id, rating, comment, created_date) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING review_id";

        if (review.getCreatedDate() == null) {
            review.setCreatedDate(LocalDate.now());
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, review.getUserId());
            stmt.setInt(2, review.getBookId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getComment());
            stmt.setDate(5, Date.valueOf(review.getCreatedDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    review.setReviewId(rs.getInt("review_id"));
                    logger.info("Review saved: id={}, user_id={}, book_id={}",
                            review.getReviewId(), review.getUserId(), review.getBookId());
                    return review;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save review", e);
            throw e;
        }
        throw new SQLException("Failed to save review");
    }

    @Override
    public Optional<Review> findById(Integer reviewId) throws SQLException {
        String sql = "SELECT * FROM review WHERE review_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find review by id: {}", reviewId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Review> findAll() throws SQLException {
        String sql = "SELECT * FROM review ORDER BY created_date DESC";
        List<Review> reviews = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reviews.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all reviews", e);
            throw e;
        }
        return reviews;
    }

    /**
     * Finds all reviews for a specific book.
     *
     * @param bookId the book ID
     * @return list of reviews
     * @throws SQLException if database error occurs
     */
    public List<Review> findByBookId(int bookId) throws SQLException {
        String sql = "SELECT * FROM review WHERE book_id = ? ORDER BY created_date DESC";
        List<Review> reviews = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find reviews for book: {}", bookId, e);
            throw e;
        }
        return reviews;
    }

    /**
     * Finds all reviews written by a specific user.
     *
     * @param userId the user ID
     * @return list of reviews
     * @throws SQLException if database error occurs
     */
    public List<Review> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM review WHERE user_id = ? ORDER BY created_date DESC";
        List<Review> reviews = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find reviews for user: {}", userId, e);
            throw e;
        }
        return reviews;
    }

    /**
     * Calculates the average rating for a book.
     *
     * @param bookId the book ID
     * @return average rating (0.0 if no reviews)
     * @throws SQLException if database error occurs
     */
    public double getAverageRating(int bookId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM review WHERE book_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get average rating for book: {}", bookId, e);
            throw e;
        }
        return 0.0;
    }

    /**
     * Checks whether the user has already reviewed the given book
     * (the DB enforces this with a unique constraint uq_review_user_book).
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return true if a review already exists
     * @throws SQLException if database error occurs
     */
    public boolean existsByUserAndBook(int userId, int bookId) throws SQLException {
        String sql = "SELECT 1 FROM review WHERE user_id = ? AND book_id = ? LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check existing review for user {} book {}", userId, bookId, e);
            throw e;
        }
    }

    /**
     * Returns the total number of reviews in the system.
     *
     * @return review count
     * @throws SQLException if database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM review";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count reviews", e);
            throw e;
        }
        return 0;
    }

    @Override
    public boolean update(Review review) throws SQLException {
        String sql = "UPDATE review SET rating = ?, comment = ? WHERE review_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, review.getRating());
            stmt.setString(2, review.getComment());
            stmt.setInt(3, review.getReviewId());

            int rows = stmt.executeUpdate();
            logger.info("Review updated: id={}", review.getReviewId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update review: {}", review.getReviewId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer reviewId) throws SQLException {
        String sql = "DELETE FROM review WHERE review_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reviewId);
            int rows = stmt.executeUpdate();
            logger.info("Review deleted: id={}", reviewId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete review: {}", reviewId, e);
            throw e;
        }
    }

    private Review mapResultSet(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setBookId(rs.getInt("book_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));

        Date createdDate = rs.getDate("created_date");
        if (createdDate != null) {
            review.setCreatedDate(createdDate.toLocalDate());
        }

        return review;
    }
}
