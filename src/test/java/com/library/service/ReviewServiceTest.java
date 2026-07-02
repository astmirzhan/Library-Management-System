package com.library.service;

import com.library.service.impl.UserServiceImpl;
import com.library.service.impl.BookServiceImpl;
import com.library.service.impl.BorrowServiceImpl;
import com.library.service.impl.ReviewServiceImpl;

import com.library.dao.BorrowRecordDAO;
import com.library.dao.ReviewDAO;
import com.library.dao.UserDAO;
import com.library.model.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock ReviewDAO reviewDAO;
    @Mock BorrowRecordDAO borrowDAO;
    @Mock UserDAO userDAO;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReviewServiceImpl(reviewDAO, borrowDAO, userDAO);
    }

    // ---------- createReview ----------
    @Test
    void createReviewRejectsInvalidRating() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createReview(3, 2, 6, "text"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createReview(3, 2, 0, "text"));
    }

    @Test
    void createReviewRejectsWhenNotBorrowed() throws Exception {
        when(borrowDAO.hasUserBorrowedBook(3, 2)).thenReturn(false);
        assertThrows(IllegalStateException.class,
                () -> service.createReview(3, 2, 5, "text"));
    }

    @Test
    void createReviewRejectsDuplicate() throws Exception {
        when(borrowDAO.hasUserBorrowedBook(3, 2)).thenReturn(true);
        when(reviewDAO.existsByUserAndBook(3, 2)).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service.createReview(3, 2, 5, "text"));
    }

    @Test
    void createReviewSavesWhenValid() throws Exception {
        when(borrowDAO.hasUserBorrowedBook(3, 2)).thenReturn(true);
        when(reviewDAO.existsByUserAndBook(3, 2)).thenReturn(false);
        when(reviewDAO.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Review r = service.createReview(3, 2, 5, "Great book");
        assertEquals(5, r.getRating());
        assertEquals(3, r.getUserId());
    }

    // ---------- updateReview (ownership) ----------
    @Test
    void updateReviewRejectsNonOwner() throws Exception {
        Review r = new Review();
        r.setReviewId(1);
        r.setUserId(99);   // different owner
        when(reviewDAO.findById(1)).thenReturn(Optional.of(r));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateReview(1, 3, 4, "edit"));
    }

    @Test
    void updateReviewReturnsFalseWhenMissing() throws Exception {
        when(reviewDAO.findById(1)).thenReturn(Optional.empty());
        assertFalse(service.updateReview(1, 3, 4, "edit"));
    }

    @Test
    void updateReviewSucceedsForOwner() throws Exception {
        Review r = new Review();
        r.setReviewId(1);
        r.setUserId(3);
        when(reviewDAO.findById(1)).thenReturn(Optional.of(r));
        when(reviewDAO.update(any())).thenReturn(true);
        assertTrue(service.updateReview(1, 3, 4, "edit"));
    }

    // ---------- deleteReview ----------
    @Test
    void deleteReviewRejectsUnauthorized() throws Exception {
        Review r = new Review();
        r.setUserId(99);
        when(reviewDAO.findById(1)).thenReturn(Optional.of(r));
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteReview(1, 3, false));
    }

    @Test
    void deleteReviewAllowedForAdmin() throws Exception {
        Review r = new Review();
        r.setUserId(99);
        when(reviewDAO.findById(1)).thenReturn(Optional.of(r));
        when(reviewDAO.deleteById(1)).thenReturn(true);
        assertTrue(service.deleteReview(1, 3, true));
    }

    // ---------- getReviewsForBook enriches with user ----------
    @Test
    void getReviewsForBookEnrichesUser() throws Exception {
        Review r = new Review();
        r.setUserId(3);
        when(reviewDAO.findByBookId(2)).thenReturn(java.util.List.of(r));
        when(userDAO.findById(3)).thenReturn(Optional.empty());
        assertEquals(1, service.getReviewsForBook(2).size());
        verify(userDAO).findById(3);
    }
}
