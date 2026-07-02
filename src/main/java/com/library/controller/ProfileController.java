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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller for the current user's own profile: view and edit.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LogManager.getLogger(ProfileController.class);

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the current user's profile page.
     */
    @GetMapping
    public String profile(Model model, HttpSession session) {
        model.addAttribute("user", session.getAttribute("currentUser"));
        return "profile";
    }

    /**
     * Updates the current user's editable fields (username, email, phone).
     * Keeps the existing password hash and role untouched.
     */
    @PostMapping("/update")
    public String update(@RequestParam String username,
                         @RequestParam String email,
                         @RequestParam(required = false) String phoneNumber,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User current = (User) session.getAttribute("currentUser");

        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            String phone = phoneNumber.trim();
            if (!phone.matches("[0-9 +()-]{7,20}")) {
                redirectAttributes.addFlashAttribute("error",
                        "Invalid phone number: 7–20 characters, digits and + - ( ) space only");
                return "redirect:/profile";
            }
        }

        try {
            Optional<User> opt = userService.findById(current.getUserId());
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/profile";
            }
            User user = opt.get();           // keeps current passwordHash & role
            user.setUsername(username);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);

            userService.updateProfile(user);
            session.setAttribute("currentUser", user);   // refresh session so UI updates
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (SQLException e) {
            logger.error("Failed to update profile for user {}", current.getUserId(), e);
            redirectAttributes.addFlashAttribute("error", "System error. Please try again.");
        }
        return "redirect:/profile";
    }
}
