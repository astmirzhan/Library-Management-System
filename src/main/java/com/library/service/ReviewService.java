package com.library.service;

import com.library.dao.BorrowRecordDAO;
import com.library.dao.ReviewDAO;
import com.library.model.BorrowRecord;
import com.library.model.Review;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for review-related business logic.
 * Handles rating, commenting, and review management.
 * Business rule: users can only review books they have actually borrowed.
 */
public class ReviewService {

    private static final Logger logger = LogManager.getLogger(ReviewService.class);

    private final ReviewDAO reviewDAO;
    private final BorrowRecordDAO borrowDAO;

    public ReviewService() {
        this.reviewDAO = new ReviewDAO();
        this.borrowDAO = new BorrowRecordDAO();
    }

    public ReviewService(ReviewDAO reviewDAO, BorrowRecordDAO borrowDAO) {
        this.reviewDAO = reviewDAO;
        this.borrowDAO = borrowDAO;
    }

    /**
     * Creates a new review for a book.
     * Business rule: the user must have borrowed the book before reviewing it.
     *
     * @param userId  the reviewer user ID
     * @param bookId  the book ID
     * @param rating  the rating (1-5)
     * @param comment the review comment
     * @return the saved review
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if user has not borrowed the book
     */
    public Review createReview(int userId, int bookId, int rating, String comment)
            throws SQLException {
        validateRating(rating);

        if (!hasUserBorrowedBook(userId, bookId)) {
            throw new IllegalStateException(
                    "You can only review books you have borrowed");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setBookId(bookId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedDate(LocalDate.now());

        Review saved = reviewDAO.save(review);
        logger.info("Review created: user={}, book={}, rating={}", userId, bookId, rating);
        return saved;
    }

    /**
     * Checks if a user has borrowed a specific book.
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return true if the user has borrowed the book
     * @throws SQLException if database error occurs
     */
    private boolean hasUserBorrowedBook(int userId, int bookId) throws SQLException {
        List<BorrowRecord> userBorrows = borrowDAO.findByUserId(userId);
        return userBorrows.stream()
                .anyMatch(record -> record.getBookCopy() != null
                        && record.getBookCopy().getBookId() == bookId);
    }

    /**
     * Returns all reviews for a specific book.
     *
     * @param bookId the book ID
     * @return list of reviews
     * @throws SQLException if database error occurs
     */
    public List<Review> getReviewsForBook(int bookId) throws SQLException {
        return reviewDAO.findByBookId(bookId);
    }

    /**
     * Returns all reviews written by a specific user.
     *
     * @param userId the user ID
     * @return list of reviews
     * @throws SQLException if database error occurs
     */
    public List<Review> getReviewsByUser(int userId) throws SQLException {
        return reviewDAO.findByUserId(userId);
    }

    /**
     * Calculates the average rating for a book.
     *
     * @param bookId the book ID
     * @return average rating (0.0 if no reviews)
     * @throws SQLException if database error occurs
     */
    public double getAverageRating(int bookId) throws SQLException {
        return reviewDAO.getAverageRating(bookId);
    }

    /**
     * Finds a review by ID.
     *
     * @param reviewId the review ID
     * @return Optional containing the review if found
     * @throws SQLException if database error occurs
     */
    public Optional<Review> getReviewById(int reviewId) throws SQLException {
        return reviewDAO.findById(reviewId);
    }

    /**
     * Updates an existing review.
     * Only the review's owner can update it.
     *
     * @param reviewId   the review ID
     * @param userId     the user attempting to update (for ownership check)
     * @param newRating  the new rating
     * @param newComment the new comment
     * @return true if updated successfully
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if user is not the owner
     */
    public boolean updateReview(int reviewId, int userId, int newRating, String newComment)
            throws SQLException {
        validateRating(newRating);

        Optional<Review> reviewOpt = reviewDAO.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            return false;
        }

        Review review = reviewOpt.get();
        if (review.getUserId() != userId) {
            throw new IllegalArgumentException("Only the review owner can update it");
        }

        review.setRating(newRating);
        review.setComment(newComment);

        boolean updated = reviewDAO.update(review);
        if (updated) {
            logger.info("Review updated: id={}", reviewId);
        }
        return updated;
    }

    /**
     * Deletes a review. Only the owner or an admin can delete.
     *
     * @param reviewId         the review ID
     * @param requestingUserId the user attempting the deletion
     * @param isAdmin          whether the requesting user is an admin
     * @return true if deleted successfully
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if user is not authorized
     */
    public boolean deleteReview(int reviewId, int requestingUserId, boolean isAdmin)
            throws SQLException {
        Optional<Review> reviewOpt = reviewDAO.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            return false;
        }

        Review review = reviewOpt.get();
        if (!isAdmin && review.getUserId() != requestingUserId) {
            throw new IllegalArgumentException("Not authorized to delete this review");
        }

        boolean deleted = reviewDAO.deleteById(reviewId);
        if (deleted) {
            logger.info("Review deleted: id={}, by user={}", reviewId, requestingUserId);
        }
        return deleted;
    }

    /**
     * Returns all reviews in the system (admin access).
     *
     * @return list of all reviews
     * @throws SQLException if database error occurs
     */
    public List<Review> getAllReviews() throws SQLException {
        return reviewDAO.findAll();
    }

    /**
     * Validates the rating value.
     *
     * @param rating the rating to validate
     */
    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}