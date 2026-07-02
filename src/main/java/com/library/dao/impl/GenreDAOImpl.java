package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.Genre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the GENRE table.
 * Provides CRUD operations for genres.
 */
@Repository
public class GenreDAOImpl implements GenreDAO {

    private static final Logger logger = LogManager.getLogger(GenreDAOImpl.class);
    private DatabaseConnection dbConnection;

    public GenreDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public Genre save(Genre genre) throws SQLException {
        String sql = "INSERT INTO genre (name) VALUES (?) RETURNING genre_id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, genre.getName());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    genre.setGenreId(rs.getInt("genre_id"));
                    logger.info("Genre saved: id={}, name={}", genre.getGenreId(), genre.getName());
                    return genre;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save genre: {}", genre.getName(), e);
            throw e;
        }
        throw new SQLException("Failed to save genre");
    }

    @Override
    public Optional<Genre> findById(Integer genreId) throws SQLException {
        String sql = "SELECT * FROM genre WHERE genre_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, genreId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find genre by id: {}", genreId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Genre> findAll() throws SQLException {
        String sql = "SELECT * FROM genre ORDER BY name";
        List<Genre> genres = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                genres.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all genres", e);
            throw e;
        }
        return genres;
    }

    /**
     * Finds all genres for a specific book.
     *
     * @param bookId the book ID
     * @return list of genres
     * @throws SQLException if database error occurs
     */
    public List<Genre> findByBookId(int bookId) throws SQLException {
        String sql = "SELECT g.* FROM genre g " +
                "JOIN book_genre bg ON g.genre_id = bg.genre_id " +
                "WHERE bg.book_id = ?";
        List<Genre> genres = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    genres.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find genres by book id: {}", bookId, e);
            throw e;
        }
        return genres;
    }

    @Override
    public boolean update(Genre genre) throws SQLException {
        String sql = "UPDATE genre SET name = ? WHERE genre_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, genre.getName());
            stmt.setInt(2, genre.getGenreId());

            int rows = stmt.executeUpdate();
            logger.info("Genre updated: id={}", genre.getGenreId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update genre: {}", genre.getGenreId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer genreId) throws SQLException {
        String sql = "DELETE FROM genre WHERE genre_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, genreId);
            int rows = stmt.executeUpdate();
            logger.info("Genre deleted: id={}", genreId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete genre: {}", genreId, e);
            throw e;
        }
    }

    private Genre mapResultSet(ResultSet rs) throws SQLException {
        Genre genre = new Genre();
        genre.setGenreId(rs.getInt("genre_id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}
