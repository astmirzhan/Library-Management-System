package com.library.controller;

import com.library.model.User;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.ReviewService;
import com.library.service.UserService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for admin operations: analytics dashboard, user management, reports.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);
    private static final int PAGE_SIZE = 20;
    private static final String[] MONTHS =
            {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final UserService userService;
    private final BookService bookService;
    private final BorrowService borrowService;
    private final ReviewService reviewService;

    @Autowired
    public AdminController(UserService userService, BookService bookService,
                           BorrowService borrowService, ReviewService reviewService) {
        this.userService = userService;
        this.bookService = bookService;
        this.borrowService = borrowService;
        this.reviewService = reviewService;
    }

    /**
     * Advanced admin dashboard: system-wide statistics and analytics.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        try {
            model.addAttribute("userCount", userService.getUserCount());
            model.addAttribute("bookCount", bookService.getBookCount());
            model.addAttribute("overdueCount", borrowService.getOverdueRecords().size());
            model.addAttribute("totalFines", borrowService.getTotalFines());
            model.addAttribute("reviewCount", reviewService.getReviewCount());

            // Most borrowed books (top 5) + most popular title
            List<Object[]> mostBorrowed = borrowService.getMostBorrowedBooks(5);
            model.addAttribute("mostBorrowed", mostBorrowed);
            model.addAttribute("popularTitle",
                    mostBorrowed.isEmpty() ? "—" : (String) mostBorrowed.get(0)[0]);

            // Monthly overdue chart for the current year
            int year = LocalDate.now().getYear();
            int[] monthly = borrowService.getOverdueCountsByMonth(year);
            List<Object[]> chart = new ArrayList<>();
            int max = 1;
            for (int m = 1; m <= 12; m++) {
                chart.add(new Object[]{MONTHS[m], monthly[m]});
                if (monthly[m] > max) max = monthly[m];
            }
            model.addAttribute("overdueChart", chart);
            model.addAttribute("overdueChartMax", max);
            model.addAttribute("chartYear", year);

            model.addAttribute("user", session.getAttribute("currentUser"));
            return "admin/dashboard";
        } catch (SQLException e) {
            logger.error("Failed to load admin dashboard", e);
            return "error";
        }
    }

    /**
     * Shows the user management page.
     */
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "1") int page,
                        Model model, HttpSession session) {
        try {
            List<User> users = userService.getAllUsers(page, PAGE_SIZE);
            int totalPages = (int) Math.ceil((double) userService.getUserCount() / PAGE_SIZE);
            model.addAttribute("users", users);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "admin/users";
        } catch (SQLException e) {
            logger.error("Failed to load users", e);
            return "error";
        }
    }

    /**
     * Blocks/activates a user. Admin may only manage LIBRARIAN accounts.
     */
    @PostMapping("/users/toggle/{userId}")
    public String toggleActive(@PathVariable int userId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<User> opt = userService.findById(userId);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            if (opt.get().getRole() != User.Role.LIBRARIAN) {
                redirectAttributes.addFlashAttribute("error",
                        "Admin can only block or activate librarian accounts");
                return "redirect:/admin/users";
            }
            boolean newState = !opt.get().isActive();
            userService.setUserActive(userId, newState);
            redirectAttributes.addFlashAttribute("success",
                    newState ? "Librarian activated" : "Librarian blocked");
        } catch (Exception e) {
            logger.error("Failed to toggle user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update user status");
        }
        return "redirect:/admin/users";
    }

    /**
     * Deletes a user. Admin may only delete LIBRARIAN accounts.
     */
    @PostMapping("/users/delete/{userId}")
    public String deleteUser(@PathVariable int userId,
                             RedirectAttributes redirectAttributes) {
        try {
            Optional<User> opt = userService.findById(userId);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            if (opt.get().getRole() != User.Role.LIBRARIAN) {
                redirectAttributes.addFlashAttribute("error",
                        "Admin can only delete librarian accounts");
                return "redirect:/admin/users";
            }
            boolean deleted = userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute(deleted ? "success" : "error",
                    deleted ? "Librarian deleted" : "Failed to delete librarian");
        } catch (Exception e) {
            logger.error("Failed to delete user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete librarian");
        }
        return "redirect:/admin/users";
    }

    /**
     * Creates a new librarian account.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/librarians")
    public String addLibrarian(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam(required = false) String phoneNumber,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.register(username, email, password, User.Role.LIBRARIAN, phoneNumber);
            redirectAttributes.addFlashAttribute("success", "Librarian created: " + username);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (SQLException e) {
            logger.error("Failed to create librarian", e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
        }
        return "redirect:/admin/users";
    }
}