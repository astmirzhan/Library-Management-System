package com.library.controller;

import com.library.model.Book;
import com.library.model.Genre;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for browsing the book catalog and viewing book details.
 */
@Controller
@RequestMapping("/catalog")
public class BookController {

    private static final Logger logger = LogManager.getLogger(BookController.class);
    private static final int PAGE_SIZE = 12;

    private final BookService bookService;
    private final ReviewService reviewService;

    @Autowired
    public BookController(BookService bookService, ReviewService reviewService) {
        this.bookService = bookService;
        this.reviewService = reviewService;
    }

    /**
     * Shows the catalog page with pagination and optional filters.
     */
    @GetMapping
    public String catalog(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(required = false) String query,
                          @RequestParam(required = false) Integer genreId,
                          @RequestParam(required = false) String availability,
                          Model model,
                          HttpSession session) {
        try {
            List<Book> books;

            if (query != null && !query.trim().isEmpty()) {
                books = bookService.searchBooks(query, page, PAGE_SIZE);
            } else if (genreId != null) {
                books = bookService.getBooksByGenre(genreId, page, PAGE_SIZE);
            } else if ("available".equalsIgnoreCase(availability)) {
                books = bookService.getAvailableBooks(page, PAGE_SIZE);
            } else {
                books = bookService.getAllBooks(page, PAGE_SIZE);
            }

            int totalBooks = bookService.getFilteredBookCount(query, genreId, availability);
            int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);

            List<Genre> genres = bookService.getAllGenres();

            model.addAttribute("books", books);
            model.addAttribute("genres", genres);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("query", query);
            model.addAttribute("selectedGenreId", genreId);
            model.addAttribute("selectedAvailability", availability);
            model.addAttribute("user", session.getAttribute("currentUser"));

            return "catalog";
        } catch (SQLException e) {
            logger.error("Failed to load catalog", e);
            model.addAttribute("error", "Failed to load catalog");
            return "error";
        }
    }

    /**
     * Shows book details page.
     */
    @GetMapping("/{bookId}")
    public String bookDetails(@PathVariable int bookId, Model model, HttpSession session) {
        try {
            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                return "redirect:/catalog";
            }

            Book book = bookOpt.get();
            List<Review> reviews = reviewService.getReviewsForBook(bookId);
            double avgRating = reviewService.getAverageRating(bookId);

            model.addAttribute("book", book);
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", avgRating);
            model.addAttribute("user", session.getAttribute("currentUser"));

            return "book-details";
        } catch (SQLException e) {
            logger.error("Failed to load book details for id={}", bookId, e);
            model.addAttribute("error", "Failed to load book details");
            return "error";
        }
    }
}