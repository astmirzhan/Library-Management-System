package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.Author;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the AUTHOR table.
 * Provides CRUD operations for authors.
 */
@Repository
public class AuthorDAOImpl implements AuthorDAO {

    private static final Logger logger = LogManager.getLogger(AuthorDAOImpl.class);
    private final DatabaseConnection dbConnection;

    public AuthorDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public Author save(Author author) throws SQLException {
        String sql = "INSERT INTO author (first_name, last_name, nationality, bio) " +
                "VALUES (?, ?, ?, ?) RETURNING author_id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, author.getFirstName());
            stmt.setString(2, author.getLastName());
            stmt.setString(3, author.getNationality());
            stmt.setString(4, author.getBio());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    author.setAuthorId(rs.getInt("author_id"));
                    logger.info("Author saved: id={}, name={}", author.getAuthorId(), author.getFullName());
                    return author;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save author: {}", author.getFullName(), e);
            throw e;
        }
        throw new SQLException("Failed to save author");
    }

    @Override
    public Optional<Author> findById(Integer authorId) throws SQLException {
        String sql = "SELECT * FROM author WHERE author_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, authorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find author by id: {}", authorId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Author> findAll() throws SQLException {
        String sql = "SELECT * FROM author ORDER BY last_name, first_name";
        List<Author> authors = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                authors.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all authors", e);
            throw e;
        }
        return authors;
    }

    /**
     * Finds all authors for a specific book.
     *
     * @param bookId the book ID
     * @return list of authors
     * @throws SQLException if database error occurs
     */
    public List<Author> findByBookId(int bookId) throws SQLException {
        String sql = "SELECT a.* FROM author a " +
                "JOIN book_author ba ON a.author_id = ba.author_id " +
                "WHERE ba.book_id = ?";
        List<Author> authors = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    authors.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find authors by book id: {}", bookId, e);
            throw e;
        }
        return authors;
    }

    @Override
    public boolean update(Author author) throws SQLException {
        String sql = "UPDATE author SET first_name = ?, last_name = ?, nationality = ?, bio = ? " +
                "WHERE author_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, author.getFirstName());
            stmt.setString(2, author.getLastName());
            stmt.setString(3, author.getNationality());
            stmt.setString(4, author.getBio());
            stmt.setInt(5, author.getAuthorId());

            int rows = stmt.executeUpdate();
            logger.info("Author updated: id={}", author.getAuthorId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update author: {}", author.getAuthorId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer authorId) throws SQLException {
        String sql = "DELETE FROM author WHERE author_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, authorId);
            int rows = stmt.executeUpdate();
            logger.info("Author deleted: id={}", authorId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete author: {}", authorId, e);
            throw e;
        }
    }

    private Author mapResultSet(ResultSet rs) throws SQLException {
        Author author = new Author();
        author.setAuthorId(rs.getInt("author_id"));
        author.setFirstName(rs.getString("first_name"));
        author.setLastName(rs.getString("last_name"));
        author.setNationality(rs.getString("nationality"));
        author.setBio(rs.getString("bio"));
        return author;
    }
}
