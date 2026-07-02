package com.library.service;

import com.library.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for user-related business logic:
 * authentication, registration and account management.
 */
public interface UserService {

    Optional<User> authenticate(String email, String plainPassword) throws SQLException;

    User register(String username, String email, String plainPassword,
                  User.Role role, String phoneNumber) throws SQLException;

    Optional<User> findById(int userId) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    List<User> getAllUsers(int page, int pageSize) throws SQLException;

    List<User> getUsersByRole(User.Role role) throws SQLException;

    boolean updateProfile(User user) throws SQLException;

    boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException;

    boolean deleteUser(int userId) throws SQLException;

    boolean setUserActive(int userId, boolean active) throws SQLException;

    int getUserCount() throws SQLException;
}
