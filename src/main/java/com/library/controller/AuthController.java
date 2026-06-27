package com.library.controller;

import com.library.model.User;
import com.library.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller for authentication: login, logout, and registration.
 */
@Controller
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);
    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the login page.
     */
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    /**
     * Processes login form submission.
     */
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        try {
            Optional<User> userOpt = userService.authenticate(email, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                HttpSession session = request.getSession(true);
                session.setAttribute("currentUser", user);
                logger.info("User logged in: {}", user.getEmail());
                return redirectByRole(user.getRole());
            } else {
                model.addAttribute("error", "Invalid email or password");
                return "login";
            }
        } catch (SQLException e) {
            logger.error("Login error", e);
            model.addAttribute("error", "System error. Please try again.");
            return "login";
        }
    }

    /**
     * Shows the registration page.
     */
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "register";
    }

    /**
     * Processes registration form submission.
     */
    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String phoneNumber,
                           Model model) {
        try {
            userService.register(username, email, password, User.Role.READER, phoneNumber);
            logger.info("New user registered: {}", email);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (SQLException e) {
            logger.error("Registration error", e);
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    /**
     * Logs the user out.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            logger.info("User logged out: {}", user.getEmail());
        }
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * Redirects user to the appropriate dashboard based on their role.
     */
    private String redirectByRole(User.Role role) {
        switch (role) {
            case ADMIN:
                return "redirect:/admin/dashboard";
            case LIBRARIAN:
                return "redirect:/librarian/dashboard";
            case READER:
            default:
                return "redirect:/catalog";
        }
    }
}