package com.library.controller;

import com.library.model.Review;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.ReviewService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for submitting book reviews.
 * Business rule (enforced in ReviewService): a user may only review a book they have borrowed.
 */
@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private static final Logger logger = LogManager.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final BookService bookService;

    @Autowired
    public ReviewController(ReviewService reviewService, BookService bookService) {
        this.reviewService = reviewService;
        this.bookService = bookService;
    }

    /**
     * Updates one of the current user's own reviews (rating + comment).
     */
    @PostMapping("/update/{reviewId}")
    public String updateReview(@PathVariable int reviewId,
                               @RequestParam int rating,
                               @RequestParam(required = false) String comment,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        try {
            boolean ok = reviewService.updateReview(reviewId, user.getUserId(), rating, comment);
            if (ok) {
                redirectAttributes.addFlashAttribute("success", "Review updated");
            } else {
                redirectAttributes.addFlashAttribute("error", "Review not found");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (SQLException e) {
            logger.error("Failed to update review {}", reviewId, e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
        }
        return "redirect:/reviews/my";
    }

    /**
     * Shows all reviews written by the current user.
     */
    @GetMapping("/my")
    public String myReviews(Model model, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        try {
            List<Review> reviews = reviewService.getReviewsByUser(user.getUserId());
            for (Review review : reviews) {
                bookService.getBookById(review.getBookId()).ifPresent(review::setBook);
            }
            model.addAttribute("reviews", reviews);
            model.addAttribute("user", user);
            return "my-reviews";
        } catch (SQLException e) {
            logger.error("Failed to load reviews for user {}", user.getUserId(), e);
            model.addAttribute("error", "Failed to load your reviews");
            return "error";
        }
    }

    /**
     * Creates a review for a book, then redirects back to the book details page.
     */
    @PostMapping
    public String create(@RequestParam int bookId,
                         @RequestParam int rating,
                         @RequestParam(required = false) String comment,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        try {
            reviewService.createReview(user.getUserId(), bookId, rating, comment);
            redirectAttributes.addFlashAttribute("success", "Review submitted. Thank you!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (SQLException e) {
            logger.error("Failed to create review for book {}", bookId, e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
        }
        return "redirect:/catalog/" + bookId;
    }
}
