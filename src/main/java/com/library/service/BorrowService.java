package com.library.service;

import com.library.dao.BookCopyDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.UserDAO;
import com.library.model.BookCopy;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for borrowing operations.
 * Handles the borrow lifecycle: request → approve → return.
 * Enforces business rules: max borrowing limit, due date calculation, fines.
 */
public class BorrowService {

    private static final Logger logger = LogManager.getLogger(BorrowService.class);

    private final BorrowRecordDAO borrowDAO;
    private final BookCopyDAO copyDAO;
    private final UserDAO userDAO;
    private final ConfigLoader config;

    public BorrowService() {
        this.borrowDAO = new BorrowRecordDAO();
        this.copyDAO = new BookCopyDAO();
        this.userDAO = new UserDAO();
        this.config = ConfigLoader.getInstance();
    }

    public BorrowService(BorrowRecordDAO borrowDAO, BookCopyDAO copyDAO,
                         UserDAO userDAO, ConfigLoader config) {
        this.borrowDAO = borrowDAO;
        this.copyDAO = copyDAO;
        this.userDAO = userDAO;
        this.config = config;
    }

    /**
     * Reader requests to borrow a book.
     * Creates a BorrowRecord with status REQUESTED.
     * Enforces the max borrowing limit per user.
     *
     * @param userId the reader user ID
     * @param bookId the book ID
     * @return the created borrow record
     * @throws SQLException             if database error occurs
     * @throws IllegalStateException    if limit exceeded or no copies available
     * @throws IllegalArgumentException if user not found
     */
    public BorrowRecord requestBorrow(int userId, int bookId) throws SQLException {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        int maxLimit = config.getMaxBorrowingLimit();
        int activeCount = borrowDAO.countActiveByUserId(userId);
        if (activeCount >= maxLimit) {
            logger.warn("User {} exceeded max borrowing limit ({})", userId, maxLimit);
            throw new IllegalStateException(
                    "Maximum borrowing limit reached (" + maxLimit + " books)");
        }

        Optional<BookCopy> copyOpt = copyDAO.findAvailableCopy(bookId);
        if (copyOpt.isEmpty()) {
            logger.warn("No available copies for book {}", bookId);
            throw new IllegalStateException("No available copies for this book");
        }

        BookCopy copy = copyOpt.get();
        int borrowDays = config.getDefaultBorrowPeriodDays();

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setCopyId(copy.getCopyId());
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(borrowDays));
        record.setStatus(BorrowRecord.Status.REQUESTED);
        record.setFineAmount(BigDecimal.ZERO);
        record.setFinePaid(false);

        BorrowRecord saved = borrowDAO.save(record);
        logger.info("Borrow requested: user={}, book={}, copy={}", userId, bookId, copy.getCopyId());
        return saved;
    }

    /**
     * Librarian approves a borrow request.
     *
     * @param borrowId    the borrow record ID
     * @param librarianId the user ID of the librarian/admin approving
     * @return true if approved successfully
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if approver doesn't have permission
     */
    public boolean approveBorrow(int borrowId, int librarianId) throws SQLException {
        Optional<User> approver = userDAO.findById(librarianId);
        if (approver.isEmpty()) {
            throw new IllegalArgumentException("Approver not found");
        }

        User user = approver.get();
        if (user.getRole() != User.Role.LIBRARIAN && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only LIBRARIAN or ADMIN can approve borrows");
        }

        return borrowDAO.approve(borrowId, librarianId);
    }

    /**
     * Returns a borrowed book and calculates fine if overdue.
     *
     * @param borrowId the borrow record ID
     * @return the updated borrow record with fine
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if borrow record not found
     */
    public BorrowRecord returnBook(int borrowId) throws SQLException {
        Optional<BorrowRecord> recordOpt = borrowDAO.findById(borrowId);
        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("Borrow record not found: " + borrowId);
        }

        BorrowRecord record = recordOpt.get();
        if (record.getReturnDate() != null) {
            throw new IllegalStateException("Book already returned");
        }

        BigDecimal fine = calculateFine(record.getDueDate(), LocalDate.now());
        borrowDAO.returnBook(borrowId, fine);

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowRecord.Status.RETURNED);
        record.setFineAmount(fine);

        logger.info("Book returned: borrowId={}, fine={}", borrowId, fine);
        return record;
    }

    /**
     * Calculates fine based on overdue days.
     * Returns BigDecimal.ZERO if not overdue.
     *
     * @param dueDate    the due date
     * @param returnDate the actual return date
     * @return the fine amount
     */
    public BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        if (returnDate == null || dueDate == null || !returnDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        double dailyFine = config.getDailyFineAmount();

        return BigDecimal.valueOf(overdueDays * dailyFine)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns all active (not returned) borrows for a user.
     *
     * @param userId the user ID
     * @return list of active borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getActiveBorrows(int userId) throws SQLException {
        return borrowDAO.findActiveByUserId(userId);
    }

    /**
     * Returns full borrowing history for a user.
     *
     * @param userId the user ID
     * @return list of borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getBorrowHistory(int userId) throws SQLException {
        return borrowDAO.findByUserId(userId);
    }

    /**
     * Returns all pending borrow requests (for librarian dashboard).
     *
     * @return list of pending requests
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getPendingRequests() throws SQLException {
        return borrowDAO.findPendingRequests();
    }

    /**
     * Returns all overdue borrow records (for librarian/admin).
     *
     * @return list of overdue records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getOverdueRecords() throws SQLException {
        return borrowDAO.findOverdue();
    }

    /**
     * Returns all borrow records with pagination (for admin reports).
     *
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of borrow records
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getAllBorrows(int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return borrowDAO.findAll(pageSize, offset);
    }

    /**
     * Marks a fine as paid.
     *
     * @param borrowId the borrow record ID
     * @return true if updated successfully
     * @throws SQLException if database error occurs
     */
    public boolean payFine(int borrowId) throws SQLException {
        Optional<BorrowRecord> recordOpt = borrowDAO.findById(borrowId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        BorrowRecord record = recordOpt.get();
        record.setFinePaid(true);
        boolean updated = borrowDAO.update(record);
        if (updated) {
            logger.info("Fine paid for borrow record: {}", borrowId);
        }
        return updated;
    }

    /**
     * Checks if a user can borrow more books.
     *
     * @param userId the user ID
     * @return true if user is within the limit
     * @throws SQLException if database error occurs
     */
    public boolean canBorrow(int userId) throws SQLException {
        int activeCount = borrowDAO.countActiveByUserId(userId);
        return activeCount < config.getMaxBorrowingLimit();
    }
}