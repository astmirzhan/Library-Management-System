package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.BorrowRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the BORROW_RECORD table.
 * Manages the borrowing lifecycle: request, approval, return.
 */
@Repository
public class BorrowRecordDAOImpl implements BorrowRecordDAO {

    private static final Logger logger = LogManager.getLogger(BorrowRecordDAOImpl.class);
    private static final int DEFAULT_LIMIT = 100;
    private final DatabaseConnection dbConnection;

    public BorrowRecordDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public BorrowRecord save(BorrowRecord record) throws SQLException {
        String sql = "INSERT INTO borrow_record (user_id, copy_id, borrow_date, due_date, " +
                "return_date, status, fine_amount, fine_paid, approved_by, approval_date) " +
                "VALUES (?, ?, ?, ?, ?, ?::borrow_status, ?, ?, ?, ?) RETURNING borrow_id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, record.getUserId());
            stmt.setInt(2, record.getCopyId());
            stmt.setDate(3, Date.valueOf(record.getBorrowDate()));
            stmt.setDate(4, Date.valueOf(record.getDueDate()));

            if (record.getReturnDate() != null) {
                stmt.setDate(5, Date.valueOf(record.getReturnDate()));
            } else {
                stmt.setNull(5, Types.DATE);
            }

            stmt.setString(6, record.getStatus().name());
            stmt.setBigDecimal(7, record.getFineAmount() != null ? record.getFineAmount() : BigDecimal.ZERO);
            stmt.setBoolean(8, record.isFinePaid());

            if (record.getApprovedBy() != null) {
                stmt.setInt(9, record.getApprovedBy());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }

            if (record.getApprovalDate() != null) {
                stmt.setDate(10, Date.valueOf(record.getApprovalDate()));
            } else {
                stmt.setNull(10, Types.DATE);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    record.setBorrowId(rs.getInt("borrow_id"));
                    logger.info("BorrowRecord saved: id={}, user_id={}, copy_id={}",
                            record.getBorrowId(), record.getUserId(), record.getCopyId());
                    return record;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save borrow record", e);
            throw e;
        }
        throw new SQLException("Failed to save borrow record");
    }

    @Override
    public Optional<BorrowRecord> findById(Integer borrowId) throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE borrow_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, borrowId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find borrow record by id: {}", borrowId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<BorrowRecord> findAll() throws SQLException {
        return findAll(DEFAULT_LIMIT, 0);
    }

    /**
     * Returns the total number of borrow records (for pagination).
     *
     * @return total borrow record count
     * @throws SQLException if database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_record";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count borrow records", e);
            throw e;
        }
        return 0;
    }

    /**
     * Returns borrow records with pagination.
     *
     * @param limit  max records per page
     * @param offset records to skip
     * @return list of borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM borrow_record ORDER BY borrow_date DESC LIMIT ? OFFSET ?";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find borrow records", e);
            throw e;
        }
        return records;
    }

    /**
     * Finds all borrow records for a specific user.
     *
     * @param userId the user ID
     * @return list of borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE user_id = ? ORDER BY borrow_date DESC";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find borrow records for user: {}", userId, e);
            throw e;
        }
        return records;
    }

    /**
     * Checks whether a user has ever borrowed any copy of a given book.
     * Joins borrow_record to book_copy to resolve copy -> book.
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return true if the user has a borrow record for a copy of the book
     * @throws SQLException if database error occurs
     */
    public boolean hasUserBorrowedBook(int userId, int bookId) throws SQLException {
        String sql = "SELECT 1 FROM borrow_record br " +
                "JOIN book_copy bc ON bc.copy_id = br.copy_id " +
                "WHERE br.user_id = ? AND bc.book_id = ? LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check borrow history for user {} book {}", userId, bookId, e);
            throw e;
        }
    }

    /**
     * Checks whether a user currently holds (or has requested) any copy of a given book —
     * i.e. an active borrow that is not yet returned. Used to prevent borrowing the same
     * book twice at once.
     *
     * @param userId the user ID
     * @param bookId the book ID
     * @return true if the user has an active borrow/request for the book
     * @throws SQLException if database error occurs
     */
    public boolean hasActiveBorrowForBook(int userId, int bookId) throws SQLException {
        String sql = "SELECT 1 FROM borrow_record br " +
                "JOIN book_copy bc ON bc.copy_id = br.copy_id " +
                "WHERE br.user_id = ? AND bc.book_id = ? AND br.return_date IS NULL " +
                "AND br.status IN ('REQUESTED', 'APPROVED', 'RETURN_REQUESTED') LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check active borrow for user {} book {}", userId, bookId, e);
            throw e;
        }
    }

    /**
     * Finds active (not returned) borrow records for a user.
     *
     * @param userId the user ID
     * @return list of active borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE user_id = ? AND return_date IS NULL " +
                "ORDER BY due_date";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find active borrows for user: {}", userId, e);
            throw e;
        }
        return records;
    }

    /**
     * Finds all pending requests (status = REQUESTED) for librarian to process.
     *
     * @return list of pending borrow requests
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findPendingRequests() throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE status = 'REQUESTED' ORDER BY borrow_date";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find pending requests", e);
            throw e;
        }
        return records;
    }

    /**
     * Finds all overdue records (active and past due date).
     *
     * @return list of overdue records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findOverdue() throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE return_date IS NULL " +
                "AND due_date < CURRENT_DATE AND status = 'APPROVED' ORDER BY due_date";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find overdue records", e);
            throw e;
        }
        return records;
    }

    /**
     * Counts currently active loans across all users (APPROVED and not returned).
     *
     * @return number of active loans
     * @throws SQLException if database error occurs
     */
    public int countActiveLoans() throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_record WHERE return_date IS NULL AND status = 'APPROVED'";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count active loans", e);
            throw e;
        }
        return 0;
    }

    /**
     * Counts physical copies that are currently held (any not-returned active borrow).
     *
     * @return number of borrowed copies
     * @throws SQLException if database error occurs
     */
    public int countBorrowedCopies() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT copy_id) FROM borrow_record " +
                "WHERE return_date IS NULL AND status IN ('REQUESTED','APPROVED','RETURN_REQUESTED')";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count borrowed copies", e);
            throw e;
        }
        return 0;
    }

    /**
     * Finds all pending return requests (status = RETURN_REQUESTED) for librarian confirmation.
     *
     * @return list of records awaiting return confirmation
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> findReturnRequested() throws SQLException {
        String sql = "SELECT * FROM borrow_record WHERE status = 'RETURN_REQUESTED' ORDER BY due_date";
        List<BorrowRecord> records = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find return requests", e);
            throw e;
        }
        return records;
    }

    /**
     * Counts active (not returned) borrow records for a user.
     * Used to enforce max borrowing limit.
     *
     * @param userId the user ID
     * @return count of active borrows
     * @throws SQLException if database error occurs
     */
    public int countActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_record WHERE user_id = ? " +
                "AND return_date IS NULL AND status IN ('REQUESTED', 'APPROVED', 'RETURN_REQUESTED')";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count active borrows for user: {}", userId, e);
            throw e;
        }
        return 0;
    }

    /**
     * Checks whether a book has any active borrow (requested / approved / return pending)
     * across all its copies. Used to prevent deleting a book that is in use.
     *
     * @param bookId the book ID
     * @return true if at least one copy has an active, not-returned borrow
     * @throws SQLException if database error occurs
     */
    public boolean hasActiveBorrowsForBook(int bookId) throws SQLException {
        String sql = "SELECT 1 FROM borrow_record br " +
                "JOIN book_copy bc ON bc.copy_id = br.copy_id " +
                "WHERE bc.book_id = ? AND br.return_date IS NULL " +
                "AND br.status IN ('REQUESTED','APPROVED','RETURN_REQUESTED') LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check active borrows for book {}", bookId, e);
            throw e;
        }
    }

    /**
     * Returns the total of all fines charged across the library.
     *
     * @return total fine amount
     * @throws SQLException if database error occurs
     */
    public BigDecimal totalFines() throws SQLException {
        String sql = "SELECT COALESCE(SUM(fine_amount), 0) FROM borrow_record";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to sum fines", e);
            throw e;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns the most borrowed books as [title, borrowCount] rows, ranked by loan count.
     *
     * @param limit max rows
     * @return list of Object[]{String title, Long count}
     * @throws SQLException if database error occurs
     */
    public List<Object[]> mostBorrowedBooks(int limit) throws SQLException {
        String sql = "SELECT b.title, COUNT(*) AS cnt " +
                "FROM borrow_record br " +
                "JOIN book_copy bc ON bc.copy_id = br.copy_id " +
                "JOIN book b ON b.book_id = bc.book_id " +
                "GROUP BY b.book_id, b.title ORDER BY cnt DESC LIMIT ?";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString(1), rs.getLong(2)});
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load most borrowed books", e);
            throw e;
        }
        return rows;
    }

    /**
     * Returns overdue-borrow counts per month (1-12) for the given year.
     * A record counts as overdue if its due date is in that month and it was
     * returned late or is still not returned past due.
     *
     * @param year the calendar year
     * @return int[13] where index 1..12 holds the month counts (index 0 unused)
     * @throws SQLException if database error occurs
     */
    public int[] overdueCountsByMonth(int year) throws SQLException {
        String sql = "SELECT EXTRACT(MONTH FROM due_date)::int AS m, COUNT(*) " +
                "FROM borrow_record " +
                "WHERE EXTRACT(YEAR FROM due_date) = ? " +
                "AND ((return_date IS NOT NULL AND return_date > due_date) " +
                "     OR (return_date IS NULL AND due_date < CURRENT_DATE)) " +
                "GROUP BY m";
        int[] months = new int[13];
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int m = rs.getInt(1);
                    if (m >= 1 && m <= 12) months[m] = rs.getInt(2);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load overdue counts by month", e);
            throw e;
        }
        return months;
    }

    /**
     * Checks whether a book has any borrow records at all (active or historical),
     * which — under the RESTRICT FK — makes physical deletion impossible.
     *
     * @param bookId the book ID
     * @return true if any borrow record references a copy of the book
     * @throws SQLException if database error occurs
     */
    public boolean hasAnyBorrowsForBook(int bookId) throws SQLException {
        String sql = "SELECT 1 FROM borrow_record br " +
                "JOIN book_copy bc ON bc.copy_id = br.copy_id " +
                "WHERE bc.book_id = ? LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check borrow history for book {}", bookId, e);
            throw e;
        }
    }

    /**
     * Rejects a pending borrow request (librarian-initiated).
     *
     * @param borrowId the borrow record ID
     * @return true if updated (was REQUESTED)
     * @throws SQLException if database error occurs
     */
    public boolean reject(int borrowId) throws SQLException {
        String sql = "UPDATE borrow_record SET status = 'REJECTED' " +
                "WHERE borrow_id = ? AND status = 'REQUESTED'";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            int rows = stmt.executeUpdate();
            logger.info("BorrowRecord rejected: id={}, rows={}", borrowId, rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to reject borrow record: {}", borrowId, e);
            throw e;
        }
    }

    /**
     * Marks a borrow record as awaiting return confirmation (reader-initiated).
     *
     * @param borrowId the borrow record ID
     * @return true if updated
     * @throws SQLException if database error occurs
     */
    public boolean requestReturn(int borrowId) throws SQLException {
        String sql = "UPDATE borrow_record SET status = 'RETURN_REQUESTED' " +
                "WHERE borrow_id = ? AND status = 'APPROVED' AND return_date IS NULL";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, borrowId);
            int rows = stmt.executeUpdate();
            logger.info("Return requested for borrow record: id={}, rows={}", borrowId, rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to request return for borrow record: {}", borrowId, e);
            throw e;
        }
    }

    /**
     * Approves a borrow request.
     *
     * @param borrowId   the borrow record ID
     * @param approvedBy the librarian/admin user ID who approves
     * @return true if approved successfully
     * @throws SQLException if database error occurs
     */
    public boolean approve(int borrowId, int approvedBy) throws SQLException {
        String sql = "UPDATE borrow_record SET status = 'APPROVED', approved_by = ?, " +
                "approval_date = ? WHERE borrow_id = ? AND status = 'REQUESTED'";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, approvedBy);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, borrowId);

            int rows = stmt.executeUpdate();
            logger.info("BorrowRecord approved: id={}, by={}", borrowId, approvedBy);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to approve borrow record: {}", borrowId, e);
            throw e;
        }
    }

    /**
     * Marks a book as returned.
     *
     * @param borrowId   the borrow record ID
     * @param fineAmount fine amount if overdue
     * @return true if returned successfully
     * @throws SQLException if database error occurs
     */
    public boolean returnBook(int borrowId, BigDecimal fineAmount) throws SQLException {
        String sql = "UPDATE borrow_record SET return_date = ?, status = 'RETURNED', " +
                "fine_amount = ? WHERE borrow_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setBigDecimal(2, fineAmount != null ? fineAmount : BigDecimal.ZERO);
            stmt.setInt(3, borrowId);

            int rows = stmt.executeUpdate();
            logger.info("Book returned for borrow record: id={}, fine={}", borrowId, fineAmount);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to return book for borrow record: {}", borrowId, e);
            throw e;
        }
    }

    @Override
    public boolean update(BorrowRecord record) throws SQLException {
        String sql = "UPDATE borrow_record SET due_date = ?, return_date = ?, status = ?::borrow_status, " +
                "fine_amount = ?, fine_paid = ? WHERE borrow_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(record.getDueDate()));

            if (record.getReturnDate() != null) {
                stmt.setDate(2, Date.valueOf(record.getReturnDate()));
            } else {
                stmt.setNull(2, Types.DATE);
            }

            stmt.setString(3, record.getStatus().name());
            stmt.setBigDecimal(4, record.getFineAmount());
            stmt.setBoolean(5, record.isFinePaid());
            stmt.setInt(6, record.getBorrowId());

            int rows = stmt.executeUpdate();
            logger.info("BorrowRecord updated: id={}", record.getBorrowId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update borrow record: {}", record.getBorrowId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer borrowId) throws SQLException {
        String sql = "DELETE FROM borrow_record WHERE borrow_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, borrowId);
            int rows = stmt.executeUpdate();
            logger.info("BorrowRecord deleted: id={}", borrowId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete borrow record: {}", borrowId, e);
            throw e;
        }
    }

    private BorrowRecord mapResultSet(ResultSet rs) throws SQLException {
        BorrowRecord record = new BorrowRecord();
        record.setBorrowId(rs.getInt("borrow_id"));
        record.setUserId(rs.getInt("user_id"));
        record.setCopyId(rs.getInt("copy_id"));

        Date borrowDate = rs.getDate("borrow_date");
        if (borrowDate != null) record.setBorrowDate(borrowDate.toLocalDate());

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) record.setDueDate(dueDate.toLocalDate());

        Date returnDate = rs.getDate("return_date");
        if (returnDate != null) record.setReturnDate(returnDate.toLocalDate());

        record.setStatus(BorrowRecord.Status.valueOf(rs.getString("status")));
        record.setFineAmount(rs.getBigDecimal("fine_amount"));
        record.setFinePaid(rs.getBoolean("fine_paid"));

        int approvedBy = rs.getInt("approved_by");
        if (!rs.wasNull()) record.setApprovedBy(approvedBy);

        Date approvalDate = rs.getDate("approval_date");
        if (approvalDate != null) record.setApprovalDate(approvalDate.toLocalDate());

        return record;
    }
}
