package com.library.controller;

import com.library.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

/**
 * Controller for the home page.
 * Redirects users to their role-specific dashboard.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        switch (user.getRole()) {
            case ADMIN:
                return "redirect:/admin/dashboard";
            case LIBRARIAN:
                return "redirect:/librarian/dashboard";
            case READER:
            default:
                return "redirect:/discover";
        }
    }
}