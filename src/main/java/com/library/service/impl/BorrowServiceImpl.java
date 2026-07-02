package com.library.service.impl;

import com.library.service.BorrowService;

import com.library.dao.BookCopyDAO;
import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.model.Book;
import com.library.dao.UserDAO;
import com.library.model.BookCopy;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
public class BorrowServiceImpl implements BorrowService {

    private static final Logger logger = LogManager.getLogger(BorrowServiceImpl.class);
    private static final int MAX_BORROW_DAYS = 60;

    private final BorrowRecordDAO borrowDAO;
    private final BookCopyDAO copyDAO;
    private final BookDAO bookDAO;
    private final UserDAO userDAO;
    private final ConfigLoader config;

    @Autowired
    public BorrowServiceImpl(BorrowRecordDAO borrowDAO, BookCopyDAO copyDAO, BookDAO bookDAO,
                         UserDAO userDAO, ConfigLoader config) {
        this.borrowDAO = borrowDAO;
        this.copyDAO = copyDAO;
        this.bookDAO = bookDAO;
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
    public BorrowRecord requestBorrow(int userId, int bookId, LocalDate dueDate) throws SQLException {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        if (userOpt.get().getRole() != User.Role.READER) {
            throw new IllegalStateException("Only readers can borrow books");
        }

        LocalDate today = LocalDate.now();
        if (dueDate == null || !dueDate.isAfter(today)) {
            throw new IllegalArgumentException("Return date must be in the future");
        }
        if (dueDate.isAfter(today.plusDays(MAX_BORROW_DAYS))) {
            throw new IllegalArgumentException(
                    "You can borrow for at most " + MAX_BORROW_DAYS + " days");
        }

        if (borrowDAO.hasActiveBorrowForBook(userId, bookId)) {
            throw new IllegalStateException("You already have this book borrowed or requested");
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

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setCopyId(copy.getCopyId());
        record.setBorrowDate(today);
        record.setDueDate(dueDate);
        record.setStatus(BorrowRecord.Status.REQUESTED);
        record.setFineAmount(BigDecimal.ZERO);
        record.setFinePaid(false);

        BorrowRecord saved = borrowDAO.save(record);
        bookDAO.decrementAvailableCopies(bookId);
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
        if (user.getRole() != User.Role.LIBRARIAN) {
            throw new IllegalArgumentException("Only a librarian can approve borrows");
        }

        return borrowDAO.approve(borrowId, librarianId);
    }

    /**
     * Librarian rejects a pending borrow request. The reserved copy is returned to the pool.
     *
     * @param borrowId the borrow record ID
     * @return true if rejected
     * @throws SQLException if database error occurs
     */
    public boolean rejectBorrow(int borrowId) throws SQLException {
        Optional<BorrowRecord> recordOpt = borrowDAO.findById(borrowId);
        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("Borrow record not found: " + borrowId);
        }
        BorrowRecord record = recordOpt.get();
        if (record.getStatus() != BorrowRecord.Status.REQUESTED) {
            throw new IllegalStateException("Only a pending request can be rejected");
        }
        boolean ok = borrowDAO.reject(borrowId);
        if (ok) {
            Optional<BookCopy> copyOpt = copyDAO.findById(record.getCopyId());
            copyOpt.ifPresent(copy -> {
                try {
                    bookDAO.incrementAvailableCopies(copy.getBookId());
                } catch (SQLException e) {
                    logger.error("Failed to free copy after reject", e);
                }
            });
        }
        return ok;
    }

    /**
     * Returns all borrow records enriched with reader and book info, optionally
     * filtered by reader username (for the librarian "Borrowing records" screen).
     *
     * @param query optional reader-name filter (case-insensitive), may be null/blank
     * @return enriched borrow records (newest first)
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getBorrowRecordsForManagement(String query) throws SQLException {
        List<BorrowRecord> records = borrowDAO.findAll(500, 0);
        List<BorrowRecord> result = new java.util.ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (BorrowRecord record : records) {
            userDAO.findById(record.getUserId()).ifPresent(record::setUser);
            Optional<BookCopy> copyOpt = copyDAO.findById(record.getCopyId());
            if (copyOpt.isPresent()) {
                BookCopy copy = copyOpt.get();
                record.setCopyNumber(copy.getCopyNumber());
                bookDAO.findById(copy.getBookId()).ifPresent(record::setBook);
            }
            if (q.isEmpty()
                    || (record.getUser() != null
                        && record.getUser().getUsername().toLowerCase().contains(q))) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * Reader requests to return a borrowed book. The book stays with the reader
     * (still counts as active) until a librarian confirms it via {@link #confirmReturn}.
     *
     * @param borrowId the borrow record ID
     * @return true if the return request was recorded
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if borrow record not found
     * @throws IllegalStateException    if the book is not currently borrowed
     */
    public boolean requestReturn(int borrowId) throws SQLException {
        Optional<BorrowRecord> recordOpt = borrowDAO.findById(borrowId);
        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("Borrow record not found: " + borrowId);
        }
        BorrowRecord record = recordOpt.get();
        if (record.getReturnDate() != null) {
            throw new IllegalStateException("Book already returned");
        }
        if (record.getStatus() != BorrowRecord.Status.APPROVED) {
            throw new IllegalStateException("Only an approved (borrowed) book can be returned");
        }
        boolean ok = borrowDAO.requestReturn(borrowId);
        logger.info("Return requested for borrow record {}", borrowId);
        return ok;
    }

    /**
     * Librarian confirms a return, records the copy condition, calculates any fine,
     * and frees up the copy.
     *
     * @param borrowId  the borrow record ID
     * @param condition the assessed condition of the returned copy (may be null to keep as-is)
     * @return the updated borrow record with fine
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if borrow record not found
     * @throws IllegalStateException    if the book was already returned
     */
    public BorrowRecord confirmReturn(int borrowId, BookCopy.Condition condition) throws SQLException {
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

        Optional<BookCopy> copyOpt = copyDAO.findById(record.getCopyId());
        if (copyOpt.isPresent()) {
            BookCopy copy = copyOpt.get();
            if (condition != null) {
                copy.setCondition(condition);
                copyDAO.update(copy);
            }
            bookDAO.incrementAvailableCopies(copy.getBookId());
        }

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowRecord.Status.RETURNED);
        record.setFineAmount(fine);

        logger.info("Return confirmed: borrowId={}, condition={}, fine={}", borrowId, condition, fine);
        return record;
    }

    /**
     * Returns all pending return requests (status RETURN_REQUESTED) for librarian confirmation.
     *
     * @return list of borrow records awaiting return confirmation
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getReturnRequests() throws SQLException {
        return borrowDAO.findReturnRequested();
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
     * Returns all borrow records for a user, enriched with book title and copy number
     * (for the "My borrowed books" screen).
     *
     * @param userId the user ID
     * @return enriched borrow records (newest first)
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getUserBorrowRecords(int userId) throws SQLException {
        List<BorrowRecord> records = borrowDAO.findByUserId(userId);
        for (BorrowRecord record : records) {
            Optional<BookCopy> copyOpt = copyDAO.findById(record.getCopyId());
            if (copyOpt.isPresent()) {
                BookCopy copy = copyOpt.get();
                record.setCopyNumber(copy.getCopyNumber());
                Optional<Book> bookOpt = bookDAO.findById(copy.getBookId());
                bookOpt.ifPresent(record::setBook);
            }
        }
        return records;
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
     * Returns the total number of borrow records (for pagination).
     *
     * @return total borrow record count
     * @throws SQLException if database error occurs
     */
    public int getBorrowCount() throws SQLException {
        return borrowDAO.count();
    }

    /**
     * Number of currently active loans (APPROVED, not returned).
     */
    public int getActiveLoanCount() throws SQLException {
        return borrowDAO.countActiveLoans();
    }

    /**
     * Total of all fines charged (for admin financial overview).
     */
    public BigDecimal getTotalFines() throws SQLException {
        return borrowDAO.totalFines();
    }

    /**
     * Most borrowed books as [title, count] rows.
     */
    public List<Object[]> getMostBorrowedBooks(int limit) throws SQLException {
        return borrowDAO.mostBorrowedBooks(limit);
    }

    /**
     * Overdue counts per month for the given year (index 1..12).
     */
    public int[] getOverdueCountsByMonth(int year) throws SQLException {
        return borrowDAO.overdueCountsByMonth(year);
    }

    /**
     * Number of physical copies currently held (any active borrow).
     */
    public int getBorrowedCopyCount() throws SQLException {
        return borrowDAO.countBorrowedCopies();
    }

    /**
     * Returns the most recent borrow records, enriched with reader and book info,
     * for the librarian dashboard.
     *
     * @param limit maximum number of records
     * @return recent borrow records (newest first)
     * @throws SQLException if database error occurs
     */
    public List<BorrowRecord> getRecentBorrows(int limit) throws SQLException {
        List<BorrowRecord> records = borrowDAO.findAll(limit, 0);
        for (BorrowRecord record : records) {
            userDAO.findById(record.getUserId()).ifPresent(record::setUser);
            Optional<BookCopy> copyOpt = copyDAO.findById(record.getCopyId());
            if (copyOpt.isPresent()) {
                BookCopy copy = copyOpt.get();
                record.setCopyNumber(copy.getCopyNumber());
                bookDAO.findById(copy.getBookId()).ifPresent(record::setBook);
            }
        }
        return records;
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