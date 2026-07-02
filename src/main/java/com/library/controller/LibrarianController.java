package com.library.controller;

import com.library.model.Book;
import com.library.model.BookCopy;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.BorrowService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Optional;

/**
 * Controller for librarian operations: manage books, approve borrows.
 */
@Controller
@RequestMapping("/librarian")
public class LibrarianController {

    private static final Logger logger = LogManager.getLogger(LibrarianController.class);
    private static final int PAGE_SIZE = 20;

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
            int totalTitles = bookService.getBookCount();
            int totalCopies = bookService.getCopyCount();
            int activeLoans = borrowService.getActiveLoanCount();
            int overdueCount = borrowService.getOverdueRecords().size();

            int borrowed = borrowService.getBorrowedCopyCount();
            int damaged = bookService.getCopyCountByCondition(BookCopy.Condition.DAMAGED);
            int worn = bookService.getCopyCountByCondition(BookCopy.Condition.WORN);
            int available = Math.max(0, totalCopies - borrowed - damaged - worn);
            int healthMax = Math.max(1, totalCopies);

            model.addAttribute("totalTitles", totalTitles);
            model.addAttribute("totalCopies", totalCopies);
            model.addAttribute("activeLoans", activeLoans);
            model.addAttribute("overdueCount", overdueCount);

            model.addAttribute("healthAvailable", available);
            model.addAttribute("healthBorrowed", borrowed);
            model.addAttribute("healthWorn", worn);
            model.addAttribute("healthDamaged", damaged);
            model.addAttribute("healthMax", healthMax);

            model.addAttribute("recentBorrows", borrowService.getRecentBorrows(6));
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
                        @RequestParam(required = false) String query,
                        Model model, HttpSession session) {
        try {
            List<Book> books;
            int totalBooks;
            if (query != null && !query.trim().isEmpty()) {
                books = bookService.searchBooksByTitle(query, page, PAGE_SIZE);
                totalBooks = bookService.getTitleSearchCount(query);
            } else {
                books = bookService.getAllBooks(page, PAGE_SIZE);
                totalBooks = bookService.getBookCount();
            }
            int totalPages = (int) Math.ceil((double) totalBooks / PAGE_SIZE);
            model.addAttribute("books", books);
            model.addAttribute("query", query);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/books";
        } catch (SQLException e) {
            logger.error("Failed to load books", e);
            return "error";
        }
    }

    /** True if the session user is NOT a librarian (admin/reader must not manage books). */
    private boolean notLibrarian(HttpSession session) {
        User u = (User) session.getAttribute("currentUser");
        return u == null || u.getRole() != User.Role.LIBRARIAN;
    }

    /**
     * Shows the form to add a new book.
     */
    @GetMapping("/books/new")
    public String addBookForm(Model model, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can add books");
            return "redirect:/librarian/books";
        }
        try {
            model.addAttribute("authors", bookService.getAllAuthors());
            model.addAttribute("genres", bookService.getAllGenres());
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/book-form";
        } catch (SQLException e) {
            logger.error("Failed to load add-book form", e);
            return "error";
        }
    }

    /**
     * Creates a new book with the requested number of copies.
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/books")
    public String createBook(@RequestParam String title,
                             @RequestParam String isbn,
                             @RequestParam int publicationYear,
                             @RequestParam(defaultValue = "1") int copies,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) List<Integer> authorIds,
                             @RequestParam(required = false) List<Integer> genreIds,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can add books");
            return "redirect:/librarian/books";
        }
        try {
            Book book = new Book();
            book.setTitle(title);
            book.setIsbn(isbn);
            book.setPublicationYear(publicationYear);
            book.setDescription(description);
            bookService.createBook(book, authorIds, genreIds, copies);
            redirectAttributes.addFlashAttribute("success", "Book added with " + copies + " copies");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/librarian/books/new";
        } catch (SQLException e) {
            logger.error("Failed to create book", e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
            return "redirect:/librarian/books/new";
        }
        return "redirect:/librarian/books";
    }

    /**
     * Shows the edit form for an existing book.
     */
    @GetMapping("/books/{bookId}/edit")
    public String editBookForm(@PathVariable int bookId, Model model, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can edit books");
            return "redirect:/librarian/books";
        }
        try {
            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Book not found");
                return "redirect:/librarian/books";
            }
            model.addAttribute("book", bookOpt.get());
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/book-edit";
        } catch (SQLException e) {
            logger.error("Failed to load edit form for book {}", bookId, e);
            return "error";
        }
    }

    /**
     * Saves edits to an existing book.
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/books/{bookId}/edit")
    public String editBook(@PathVariable int bookId,
                           @RequestParam String title,
                           @RequestParam String isbn,
                           @RequestParam int publicationYear,
                           @RequestParam(required = false) String description,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can edit books");
            return "redirect:/librarian/books";
        }
        try {
            bookService.editBook(bookId, title, isbn, publicationYear, description);
            redirectAttributes.addFlashAttribute("success", "Book updated");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/librarian/books/" + bookId + "/edit";
        } catch (SQLException e) {
            logger.error("Failed to update book {}", bookId, e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
            return "redirect:/librarian/books/" + bookId + "/edit";
        }
        return "redirect:/librarian/books";
    }

    /**
     * Deletes a book (blocked if it has active borrows or any borrowing history).
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/books/{bookId}/delete")
    public String deleteBook(@PathVariable int bookId, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can delete books");
            return "redirect:/librarian/books";
        }
        try {
            boolean deleted = bookService.deleteBook(bookId);
            redirectAttributes.addFlashAttribute(deleted ? "success" : "error",
                    deleted ? "Book deleted" : "Failed to delete book");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to delete book {}", bookId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete book");
        }
        return "redirect:/librarian/books";
    }

    /**
     * Adds more physical copies to an existing book.
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/books/{bookId}/copies")
    public String addCopies(@PathVariable int bookId,
                            @RequestParam(defaultValue = "1") int count,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (notLibrarian(session)) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can add copies");
            return "redirect:/librarian/books";
        }
        try {
            bookService.addCopies(bookId, count);
            redirectAttributes.addFlashAttribute("success", count + " copies added");
        } catch (Exception e) {
            logger.error("Failed to add copies to book {}", bookId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to add copies");
        }
        return "redirect:/librarian/books";
    }

    /**
     * Shows all overdue borrow records (not returned, past due date).
     */
    @GetMapping("/overdue")
    public String overdue(Model model, HttpSession session) {
        try {
            List<BorrowRecord> overdue = borrowService.getOverdueRecords();
            model.addAttribute("overdue", overdue);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "librarian/overdue";
        } catch (SQLException e) {
            logger.error("Failed to load overdue list", e);
            return "error";
        }
    }

    /**
     * Sends an overdue reminder for a borrow record (demo: logged + confirmation flash).
     */
    @PostMapping("/overdue/remind/{borrowId}")
    public String sendReminder(@PathVariable int borrowId,
                               RedirectAttributes redirectAttributes) {
        logger.info("Overdue reminder sent for borrow record {}", borrowId);
        redirectAttributes.addFlashAttribute("success", "Reminder sent for borrow #" + borrowId);
        return "redirect:/librarian/overdue";
    }

    /**
     * Shows all borrow records for management.
     */
    @GetMapping("/borrows")
    public String borrows(@RequestParam(required = false) String query,
                          Model model, HttpSession session) {
        try {
            model.addAttribute("borrows", borrowService.getBorrowRecordsForManagement(query));
            model.addAttribute("query", query);
            model.addAttribute("conditions", BookCopy.Condition.values());
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
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/borrows/approve/{borrowId}")
    public String approveBorrow(@PathVariable int borrowId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User librarian = (User) session.getAttribute("currentUser");
        try {
            boolean approved = borrowService.approveBorrow(borrowId, librarian.getUserId());
            redirectAttributes.addFlashAttribute(approved ? "success" : "error",
                    approved ? "Borrow approved" : "Failed to approve borrow");
        } catch (Exception e) {
            logger.error("Failed to approve borrow {}", borrowId, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/borrows";
    }

    /**
     * Rejects a pending borrow request.
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/borrows/reject/{borrowId}")
    public String rejectBorrow(@PathVariable int borrowId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User current = (User) session.getAttribute("currentUser");
        if (current == null || current.getRole() != User.Role.LIBRARIAN) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can reject requests");
            return "redirect:/librarian/borrows";
        }
        try {
            boolean rejected = borrowService.rejectBorrow(borrowId);
            redirectAttributes.addFlashAttribute(rejected ? "success" : "error",
                    rejected ? "Borrow request rejected" : "Failed to reject request");
        } catch (Exception e) {
            logger.error("Failed to reject borrow {}", borrowId, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/borrows";
    }

    /**
     * Registers a return directly from the borrowing records page (records copy condition).
     */
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/borrows/return/{borrowId}")
    public String registerReturn(@PathVariable int borrowId,
                                 @RequestParam(defaultValue = "GOOD") String condition,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User current = (User) session.getAttribute("currentUser");
        if (current == null || current.getRole() != User.Role.LIBRARIAN) {
            redirectAttributes.addFlashAttribute("error", "Only a librarian can register returns");
            return "redirect:/librarian/borrows";
        }
        try {
            BookCopy.Condition cond = BookCopy.Condition.valueOf(condition);
            BorrowRecord record = borrowService.confirmReturn(borrowId, cond);
            if (record.getFineAmount() != null && record.getFineAmount().signum() > 0) {
                redirectAttributes.addFlashAttribute("success",
                        "Return registered. Late fee: " + record.getFineAmount() + " ₸");
            } else {
                redirectAttributes.addFlashAttribute("success", "Return registered");
            }
        } catch (Exception e) {
            logger.error("Failed to register return {}", borrowId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to register return");
        }
        return "redirect:/librarian/borrows";
    }
}