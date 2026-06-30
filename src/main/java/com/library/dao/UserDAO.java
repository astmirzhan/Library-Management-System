package com.library.dao;

import com.library.config.DatabaseConnection;
import com.library.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for the USER table.
 * Provides CRUD operations and queries specific to users.
 */
@Repository
public class UserDAO implements BaseDAO<User, Integer> {

    private static final Logger logger = LogManager.getLogger(UserDAO.class);
    private final DatabaseConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Saves a new user to the database.
     * Auto-generates UUID and registration date if not set.
     *
     * @param user the user to save
     * @return the saved user with generated ID
     * @throws SQLException if database error occurs
     */
    @Override
    public User save(User user) throws SQLException {
        String sql = "INSERT INTO \"user\" (user_uuid, username, email, password, " +
                "role, registration_date, phone_number) VALUES (?, ?, ?, ?, ?::user_role, ?, ?) " +
                "RETURNING user_id";

        if (user.getUserUuid() == null) {
            user.setUserUuid(UUID.randomUUID().toString());
        }
        if (user.getRegistrationDate() == null) {
            user.setRegistrationDate(java.time.LocalDate.now());
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUserUuid());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getRole().name());
            stmt.setDate(6, Date.valueOf(user.getRegistrationDate()));
            stmt.setString(7, user.getPhoneNumber());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt("user_id"));
                    logger.info("User saved successfully: id={}, username={}",
                            user.getUserId(), user.getUsername());
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save user: {}", user.getUsername(), e);
            throw e;
        }
        throw new SQLException("Failed to save user, no ID returned");
    }

    /**
     * Finds a user by their ID.
     *
     * @param userId the user ID
     * @return Optional containing the user if found
     * @throws SQLException if database error occurs
     */
    @Override
    public Optional<User> findById(Integer userId) throws SQLException {
        String sql = "SELECT * FROM \"user\" WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by id: {}", userId, e);
            throw e;
        }
        return Optional.empty();
    }

    /**
     * Finds a user by email (used for login).
     *
     * @param email the user email
     * @return Optional containing the user if found
     * @throws SQLException if database error occurs
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM \"user\" WHERE email = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by email: {}", email, e);
            throw e;
        }
        return Optional.empty();
    }

    /**
     * Returns all users with pagination.
     *
     * @param limit  max records to return
     * @param offset records to skip
     * @return list of users
     * @throws SQLException if database error occurs
     */
    public List<User> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM \"user\" ORDER BY user_id LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find all users with pagination", e);
            throw e;
        }
        return users;
    }

    @Override
    public List<User> findAll() throws SQLException {
        return findAll(100, 0);
    }

    /**
     * Finds users by role.
     *
     * @param role the user role
     * @return list of users with the specified role
     * @throws SQLException if database error occurs
     */
    public List<User> findByRole(User.Role role) throws SQLException {
        String sql = "SELECT * FROM \"user\" WHERE role = ?::user_role ORDER BY username";
        List<User> users = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find users by role: {}", role, e);
            throw e;
        }
        return users;
    }

    /**
     * Updates an existing user.
     *
     * @param user the user to update
     * @return true if updated successfully
     * @throws SQLException if database error occurs
     */
    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE \"user\" SET username = ?, email = ?, password = ?, " +
                "role = ?::user_role, phone_number = ? WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole().name());
            stmt.setString(5, user.getPhoneNumber());
            stmt.setInt(6, user.getUserId());

            int rowsUpdated = stmt.executeUpdate();
            logger.info("User updated: id={}, rows={}", user.getUserId(), rowsUpdated);
            return rowsUpdated > 0;
        } catch (SQLException e) {
            logger.error("Failed to update user: {}", user.getUserId(), e);
            throw e;
        }
    }

    /**
     * Deletes a user by ID.
     *
     * @param userId the user ID
     * @return true if deleted successfully
     * @throws SQLException if database error occurs
     */
    @Override
    public boolean deleteById(Integer userId) throws SQLException {
        String sql = "DELETE FROM \"user\" WHERE user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            int rowsDeleted = stmt.executeUpdate();
            logger.info("User deleted: id={}, rows={}", userId, rowsDeleted);
            return rowsDeleted > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Returns the total count of users in the database.
     *
     * @return total user count
     * @throws SQLException if database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM \"user\"";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count users", e);
            throw e;
        }
        return 0;
    }

    /**
     * Maps a ResultSet row to a User object.
     *
     * @param rs the ResultSet
     * @return User object
     * @throws SQLException if column access fails
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUserUuid(rs.getString("user_uuid"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password"));
        user.setRole(User.Role.valueOf(rs.getString("role")));

        Date regDate = rs.getDate("registration_date");
        if (regDate != null) {
            user.setRegistrationDate(regDate.toLocalDate());
        }

        user.setPhoneNumber(rs.getString("phone_number"));
        return user;
    }
}