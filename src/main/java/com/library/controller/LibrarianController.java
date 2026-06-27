package com.library.controller;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.BookService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for librarian operations: manage books, approve borrows.
 */
@Controller
@RequestMapping("/librarian")
public class LibrarianController {

    private static final Logger logger = LogManager.getLogger(LibrarianController.class);

    private final BookService bookService;
    private final BorrowService borrowService;

    @Autowired
    public LibrarianController(BookService bookService, BorrowService borrowService) {
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    /**
     * Librarian dashboard with overview.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        try {
            List<BorrowRecord> pending = borrowService.getPendingRequests();
            List<BorrowRecord> overdue = borrowService.getOverdueRecords();
            int totalBooks = bookService.getBookCount();

            model.addAttribute("pendingCount", pending.size());
            model.addAttribute("overdueCount", overdue.size());
            model.addAttribute("totalBooks", totalBooks);
            model.addAttribute("pendingRequests", pending);
            model.addAttribute("user", session.getAttribute("currentUser"));

            return "librarian/dashboard";
        } catch (SQLException e) {
            logger.error("Failed to load librarian dashboard", e);
            return "error";
        }
    }

    /**
     * Shows all books for management.
     */
    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "1") int page,
                        Model model, HttpSession session) {
        try {
            List<Book> books = bookService.getAllBooks(page, 20);
            model.addAttribute("books", books);
            model.addAttribute("currentPage", page);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/books";
        } catch (SQLException e) {
            logger.error("Failed to load books", e);
            return "error";
        }
    }

    /**
     * Shows all borrow records for management.
     */
    @GetMapping("/borrows")
    public String borrows(@RequestParam(defaultValue = "1") int page,
                          Model model, HttpSession session) {
        try {
            List<BorrowRecord> borrows = borrowService.getAllBorrows(page, 20);
            model.addAttribute("borrows", borrows);
            model.addAttribute("currentPage", page);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/borrows";
        } catch (SQLException e) {
            logger.error("Failed to load borrows", e);
            return "error";
        }
    }

    /**
     * Approves a pending borrow request.
     */
    @PostMapping("/borrows/approve/{borrowId}")
    public String approveBorrow(@PathVariable int borrowId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User librarian = (User) session.getAttribute("currentUser");
        try {
            boolean approved = borrowService.approveBorrow(borrowId, librarian.getUserId());
            if (approved) {
                redirectAttributes.addFlashAttribute("success", "Borrow approved");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to approve borrow");
            }
        } catch (Exception e) {
            logger.error("Failed to approve borrow {}", borrowId, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/dashboard";
    }
}