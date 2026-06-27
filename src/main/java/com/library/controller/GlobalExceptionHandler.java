package com.library.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

/**
 * Global exception handler for the entire application.
 * Catches exceptions thrown by any controller and displays a user-friendly error page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles SQL exceptions.
     */
    @ExceptionHandler(SQLException.class)
    public String handleSQLException(SQLException ex, Model model, HttpServletRequest request) {
        logger.error("Database error at {}", request.getRequestURI(), ex);
        model.addAttribute("error", "Database error. Please try again later.");
        return "error";
    }

    /**
     * Handles validation exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model,
                                        HttpServletRequest request) {
        logger.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    /**
     * Handles business rule violations.
     */
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, Model model,
                                     HttpServletRequest request) {
        logger.warn("Business rule violation at {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    /**
     * Handles any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model, HttpServletRequest request) {
        logger.error("Unexpected error at {}", request.getRequestURI(), ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again.");
        return "error";
    }
}