package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.BorrowService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for borrowing operations from the reader's perspective.
 */
@Controller
@RequestMapping("/borrowings")
public class BorrowController {

    private static final Logger logger = LogManager.getLogger(BorrowController.class);

    private final BorrowService borrowService;

    @Autowired
    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    /**
     * Shows the reader's active and historical borrowings.
     */
    @GetMapping
    public String myBorrowings(Model model, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        try {
            List<BorrowRecord> active = borrowService.getActiveBorrows(user.getUserId());
            List<BorrowRecord> history = borrowService.getBorrowHistory(user.getUserId());

            model.addAttribute("activeBorrows", active);
            model.addAttribute("history", history);
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
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        try {
            borrowService.requestBorrow(user.getUserId(), bookId);
            redirectAttributes.addFlashAttribute("success",
                    "Borrow request submitted. Awaiting librarian approval.");
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
            BorrowRecord record = borrowService.returnBook(borrowId);
            if (record.getFineAmount() != null
                    && record.getFineAmount().signum() > 0) {
                redirectAttributes.addFlashAttribute("warning",
                        "Book returned. Late fee: $" + record.getFineAmount());
            } else {
                redirectAttributes.addFlashAttribute("success", "Book returned successfully");
            }
            return "redirect:/borrowings";
        } catch (Exception e) {
            logger.error("Failed to return book", e);
            redirectAttributes.addFlashAttribute("error", "Failed to return book");
            return "redirect:/borrowings";
        }
    }
}