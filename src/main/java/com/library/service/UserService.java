package com.library.service;

import com.library.dao.UserDAO;
import com.library.model.User;
import com.library.util.PasswordUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for user-related business logic.
 * Handles authentication, registration, and user management.
 */
public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param email         the user email
     * @param plainPassword the plain text password
     * @return Optional containing the user if authentication succeeds
     * @throws SQLException if database error occurs
     */
    public Optional<User> authenticate(String email, String plainPassword) throws SQLException {
        if (email == null || plainPassword == null) {
            logger.warn("Authentication failed: null email or password");
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: user not found for email {}", email);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
            logger.info("User authenticated: {}", email);
            return Optional.of(user);
        }

        logger.warn("Authentication failed: invalid password for email {}", email);
        return Optional.empty();
    }

    /**
     * Registers a new user with hashed password.
     *
     * @param username      the username
     * @param email         the email address
     * @param plainPassword the plain text password (will be hashed)
     * @param role          the user role
     * @param phoneNumber   the phone number
     * @return the created user
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if validation fails
     */
    public User register(String username, String email, String plainPassword,
                         User.Role role, String phoneNumber) throws SQLException {
        validateRegistration(username, email, plainPassword);

        if (userDAO.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        user.setRole(role != null ? role : User.Role.READER);
        user.setRegistrationDate(LocalDate.now());
        user.setPhoneNumber(phoneNumber);

        User saved = userDAO.save(user);
        logger.info("User registered: {} ({})", username, email);
        return saved;
    }

    /**
     * Validates registration input.
     */
    private void validateRegistration(String username, String email, String password) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    /**
     * Finds a user by ID.
     *
     * @param userId the user ID
     * @return Optional containing the user if found
     * @throws SQLException if database error occurs
     */
    public Optional<User> findById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    /**
     * Finds a user by email.
     *
     * @param email the email
     * @return Optional containing the user if found
     * @throws SQLException if database error occurs
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }

    /**
     * Returns all users with pagination.
     *
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of users
     * @throws SQLException if database error occurs
     */
    public List<User> getAllUsers(int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return userDAO.findAll(pageSize, offset);
    }

    /**
     * Finds users by role.
     *
     * @param role the role
     * @return list of users
     * @throws SQLException if database error occurs
     */
    public List<User> getUsersByRole(User.Role role) throws SQLException {
        return userDAO.findByRole(role);
    }

    /**
     * Updates a user's profile.
     *
     * @param user the user to update
     * @return true if updated successfully
     * @throws SQLException if database error occurs
     */
    public boolean updateProfile(User user) throws SQLException {
        return userDAO.update(user);
    }

    /**
     * Changes a user's password.
     *
     * @param userId      the user ID
     * @param oldPassword the current password (for verification)
     * @param newPassword the new password
     * @return true if password was changed successfully
     * @throws SQLException if database error occurs
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword)
            throws SQLException {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            logger.warn("Password change failed: invalid old password for user {}", userId);
            return false;
        }

        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        boolean updated = userDAO.update(user);
        if (updated) {
            logger.info("Password changed for user {}", userId);
        }
        return updated;
    }

    /**
     * Deletes a user.
     *
     * @param userId the user ID
     * @return true if deleted successfully
     * @throws SQLException if database error occurs
     */
    public boolean deleteUser(int userId) throws SQLException {
        return userDAO.deleteById(userId);
    }

    /**
     * Returns the total count of users.
     *
     * @return user count
     * @throws SQLException if database error occurs
     */
    public int getUserCount() throws SQLException {
        return userDAO.count();
    }
}