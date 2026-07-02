package com.library.service;

import com.library.model.Review;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for review-related business logic:
 * rating, commenting and moderation.
 */
public interface ReviewService {

    Review createReview(int userId, int bookId, int rating, String comment) throws SQLException;

    int getReviewCount() throws SQLException;

    List<Review> getReviewsForBook(int bookId) throws SQLException;

    List<Review> getReviewsByUser(int userId) throws SQLException;

    double getAverageRating(int bookId) throws SQLException;

    Optional<Review> getReviewById(int reviewId) throws SQLException;

    boolean updateReview(int reviewId, int userId, int newRating, String newComment) throws SQLException;

    boolean deleteReview(int reviewId, int requestingUserId, boolean isAdmin) throws SQLException;

    List<Review> getAllReviews() throws SQLException;
}
