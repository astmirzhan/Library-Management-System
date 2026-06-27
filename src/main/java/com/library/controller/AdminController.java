package com.library.controller;

import com.library.model.User;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.UserService;
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
 * Controller for admin operations: user management, statistics, reports.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);

    private final UserService userService;
    private final BookService bookService;
    private final BorrowService borrowService;

    @Autowired
    public AdminController(UserService userService, BookService bookService,
                           BorrowService borrowService) {
        this.userService = userService;
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    /**
     * Admin dashboard with statistics.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        try {
            int userCount = userService.getUserCount();
            int bookCount = bookService.getBookCount();
            int overdueCount = borrowService.getOverdueRecords().size();
            int pendingCount = borrowService.getPendingRequests().size();

            model.addAttribute("userCount", userCount);
            model.addAttribute("bookCount", bookCount);
            model.addAttribute("overdueCount", overdueCount);
            model.addAttribute("pendingCount", pendingCount);
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
            List<User> users = userService.getAllUsers(page, 20);
            model.addAttribute("users", users);
            model.addAttribute("currentPage", page);
            model.addAttribute("user", session.getAttribute("currentUser"));
            return "admin/users";
        } catch (SQLException e) {
            logger.error("Failed to load users", e);
            return "error";
        }
    }

    /**
     * Deletes a user.
     * Note: An admin cannot delete other admins.
     */
    @PostMapping("/users/delete/{userId}")
    public String deleteUser(@PathVariable int userId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser.getUserId() == userId) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete yourself");
            return "redirect:/admin/users";
        }

        try {
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "User deleted");
            }
        } catch (Exception e) {
            logger.error("Failed to delete user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete user");
        }
        return "redirect:/admin/users";
    }
}