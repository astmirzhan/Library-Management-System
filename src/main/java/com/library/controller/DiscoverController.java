package com.library.controller;

import com.library.model.Book;
import com.library.service.BookService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

/**
 * Reader landing page — a welcome screen featuring available books.
 */
@Controller
public class DiscoverController {

    private static final Logger logger = LogManager.getLogger(DiscoverController.class);
    private static final int FEATURED_COUNT = 6;

    private final BookService bookService;

    @Autowired
    public DiscoverController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/discover")
    public String discover(Model model, HttpSession session) {
        try {
            List<Book> featured = bookService.getAvailableBooks(1, FEATURED_COUNT);
            model.addAttribute("books", featured);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "discover";
        } catch (SQLException e) {
            logger.error("Failed to load discover page", e);
            model.addAttribute("error", "Failed to load the page");
            return "error";
        }
    }
}
