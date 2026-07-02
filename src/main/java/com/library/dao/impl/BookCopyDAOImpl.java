package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.BookCopy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the BOOK_COPY table.
 * Provides CRUD operations for physical book copies.
 */
@Repository
public class BookCopyDAOImpl implements BookCopyDAO {

    private static final Logger logger = LogManager.getLogger(BookCopyDAOImpl.class);
    private final DatabaseConnection dbConnection;

    public BookCopyDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public BookCopy save(BookCopy copy) throws SQLException {
        String sql = "INSERT INTO book_copy (book_id, copy_number, condition, acquisition_date) " +
                "VALUES (?, ?, ?::copy_condition, ?) RETURNING copy_id";

        if (copy.getAcquisitionDate() == null) {
            copy.setAcquisitionDate(LocalDate.now());
        }
        if (copy.getCondition() == null) {
            copy.setCondition(BookCopy.Condition.GOOD);
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, copy.getBookId());
            stmt.setInt(2, copy.getCopyNumber());
            stmt.setString(3, copy.getCondition().name());
            stmt.setDate(4, Date.valueOf(copy.getAcquisitionDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    copy.setCopyId(rs.getInt("copy_id"));
                    logger.info("BookCopy saved: id={}, book_id={}",
                            copy.getCopyId(), copy.getBookId());
                    return copy;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save book copy for book: {}", copy.getBookId(), e);
            throw e;
        }
        throw new SQLException("Failed to save book copy");
    }

    @Override
    public Optional<BookCopy> findById(Integer copyId) throws SQLException {
        String sql = "SELECT * FROM book_copy WHERE copy_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, copyId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find book copy by id: {}", copyId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<BookCopy> findAll() throws SQLException {
        String sql = "SELECT * FROM book_copy ORDER BY book_id, copy_number";
        List<BookCopy> copies = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                copies.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all book copies", e);
            throw e;
        }
        return copies;
    }

    /**
     * Finds all copies of a specific book.
     *
     * @param bookId the book ID
     * @return list of copies
     * @throws SQLException if database error occurs
     */
    public List<BookCopy> findByBookId(int bookId) throws SQLException {
        String sql = "SELECT * FROM book_copy WHERE book_id = ? ORDER BY copy_number";
        List<BookCopy> copies = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    copies.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find copies for book: {}", bookId, e);
            throw e;
        }
        return copies;
    }

    /**
     * Finds the first available copy of a book (not currently borrowed).
     *
     * @param bookId the book ID
     * @return Optional containing the available copy
     * @throws SQLException if database error occurs
     */
    public Optional<BookCopy> findAvailableCopy(int bookId) throws SQLException {
        String sql = "SELECT bc.* FROM book_copy bc " +
                "WHERE bc.book_id = ? " +
                "AND bc.condition != 'DAMAGED' " +
                "AND bc.copy_id NOT IN ( " +
                "    SELECT br.copy_id FROM borrow_record br " +
                "    WHERE br.return_date IS NULL " +
                "    AND br.status IN ('REQUESTED', 'APPROVED', 'RETURN_REQUESTED') " +
                ") " +
                "LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find available copy for book: {}", bookId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public boolean update(BookCopy copy) throws SQLException {
        String sql = "UPDATE book_copy SET condition = ?::copy_condition WHERE copy_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, copy.getCondition().name());
            stmt.setInt(2, copy.getCopyId());

            int rows = stmt.executeUpdate();
            logger.info("BookCopy updated: id={}", copy.getCopyId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update book copy: {}", copy.getCopyId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer copyId) throws SQLException {
        String sql = "DELETE FROM book_copy WHERE copy_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, copyId);
            int rows = stmt.executeUpdate();
            logger.info("BookCopy deleted: id={}", copyId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete book copy: {}", copyId, e);
            throw e;
        }
    }

    /**
     * Returns the total number of physical copies.
     *
     * @return total copy count
     * @throws SQLException if database error occurs
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM book_copy";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count copies", e);
            throw e;
        }
        return 0;
    }

    /**
     * Counts copies in a given condition (e.g. DAMAGED, WORN).
     *
     * @param condition the copy condition
     * @return matching copy count
     * @throws SQLException if database error occurs
     */
    public int countByCondition(BookCopy.Condition condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book_copy WHERE condition = ?::copy_condition";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, condition.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count copies by condition: {}", condition, e);
            throw e;
        }
        return 0;
    }

    private BookCopy mapResultSet(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy();
        copy.setCopyId(rs.getInt("copy_id"));
        copy.setBookId(rs.getInt("book_id"));
        copy.setCopyNumber(rs.getInt("copy_number"));
        copy.setCondition(BookCopy.Condition.valueOf(rs.getString("condition")));

        Date acqDate = rs.getDate("acquisition_date");
        if (acqDate != null) {
            copy.setAcquisitionDate(acqDate.toLocalDate());
        }

        return copy;
    }
}