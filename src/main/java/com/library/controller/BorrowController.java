package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.BorrowService;
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

import org.springframework.format.annotation.DateTimeFormat;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for borrowing operations from the reader's perspective.
 */
@Controller
@RequestMapping("/borrowings")
public class BorrowController {

    private static final Logger logger = LogManager.getLogger(BorrowController.class);

    private final BorrowService borrowService;
    private final ReviewService reviewService;

    @Autowired
    public BorrowController(BorrowService borrowService, ReviewService reviewService) {
        this.borrowService = borrowService;
        this.reviewService = reviewService;
    }

    /**
     * Shows the reader's borrowings (active + returned) with summary stats.
     */
    @GetMapping
    public String myBorrowings(Model model, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        try {
            List<BorrowRecord> records = borrowService.getUserBorrowRecords(user.getUserId());
            LocalDate soon = LocalDate.now().plusDays(7);

            long currentlyBorrowed = records.stream()
                    .filter(r -> r.getStatus() == BorrowRecord.Status.APPROVED
                            && r.getReturnDate() == null).count();
            long dueSoon = records.stream()
                    .filter(r -> r.getStatus() == BorrowRecord.Status.APPROVED
                            && r.getReturnDate() == null
                            && r.getDueDate() != null && !r.getDueDate().isAfter(soon)).count();
            long returnedCount = records.stream()
                    .filter(r -> r.getStatus() == BorrowRecord.Status.RETURNED).count();
            int reviewsWritten = reviewService.getReviewsByUser(user.getUserId()).size();

            model.addAttribute("records", records);
            model.addAttribute("currentlyBorrowed", currentlyBorrowed);
            model.addAttribute("dueSoon", dueSoon);
            model.addAttribute("returnedCount", returnedCount);
            model.addAttribute("reviewsWritten", reviewsWritten);
            model.addAttribute("user", user);

            return "my-borrowings";
        } catch (SQLException e) {
            logger.error("Failed to load borrowings for user {}", user.getUserId(), e);
            model.addAttribute("error", "Failed to load borrowings");
            return "error";
        }
    }

    /**
     * Reader requests to borrow a book.
     */
    @PostMapping("/request/{bookId}")
    public String requestBorrow(@PathVariable int bookId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        try {
            borrowService.requestBorrow(user.getUserId(), bookId, dueDate);
            redirectAttributes.addFlashAttribute("success",
                    "Borrow request submitted (return by " + dueDate + "). Awaiting librarian approval.");
            return "redirect:/borrowings";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/catalog/" + bookId;
        } catch (SQLException e) {
            logger.error("Failed to request borrow", e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
            return "redirect:/catalog/" + bookId;
        }
    }

    /**
     * Reader returns a borrowed book.
     */
    @PostMapping("/return/{borrowId}")
    public String returnBook(@PathVariable int borrowId,
                             RedirectAttributes redirectAttributes) {
        try {
            borrowService.requestReturn(borrowId);
            redirectAttributes.addFlashAttribute("success",
                    "Return requested. Awaiting librarian confirmation.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to request return", e);
            redirectAttributes.addFlashAttribute("error", "Failed to request return");
        }
        return "redirect:/borrowings";
    }
}