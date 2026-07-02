package com.library.controller;

import com.library.dto.RegisterForm;
import com.library.model.User;
import com.library.service.UserService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

/**
 * Controller for authentication: login, logout, and registration.
 */
@Controller
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

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
                        HttpServletResponse response,
                        Model model) {
        try {
            Optional<User> userOpt = userService.authenticate(email, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isActive()) {
                    model.addAttribute("error", "Your account is blocked. Contact an administrator.");
                    return "login";
                }
                HttpSession session = request.getSession(true);
                session.setAttribute("currentUser", user);
                establishSecurityContext(user, request, response);
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
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "register";
        }
        try {
            userService.register(form.getUsername(), form.getEmail(), form.getPassword(),
                    User.Role.READER, form.getPhoneNumber());
            logger.info("New user registered: {}", form.getEmail());
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
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * Populates the Spring Security context with the authenticated user's role
     * (as ROLE_&lt;ROLE&gt;) and persists it to the session, so URL rules and
     * {@code @PreAuthorize} annotations can enforce authorization on later requests.
     */
    private void establishSecurityContext(User user, HttpServletRequest request,
                                          HttpServletResponse response) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
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
                return "redirect:/discover";
        }
    }
}