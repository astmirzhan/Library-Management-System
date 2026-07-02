package com.library.service;

import com.library.model.BookCopy;
import com.library.model.BorrowRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for the borrowing lifecycle:
 * request → approval → return, plus fines and reporting.
 */
public interface BorrowService {

    BorrowRecord requestBorrow(int userId, int bookId, LocalDate dueDate) throws SQLException;

    boolean approveBorrow(int borrowId, int librarianId) throws SQLException;

    boolean rejectBorrow(int borrowId) throws SQLException;

    List<BorrowRecord> getBorrowRecordsForManagement(String query) throws SQLException;

    boolean requestReturn(int borrowId) throws SQLException;

    BorrowRecord confirmReturn(int borrowId, BookCopy.Condition condition) throws SQLException;

    List<BorrowRecord> getReturnRequests() throws SQLException;

    BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate);

    List<BorrowRecord> getActiveBorrows(int userId) throws SQLException;

    List<BorrowRecord> getBorrowHistory(int userId) throws SQLException;

    List<BorrowRecord> getUserBorrowRecords(int userId) throws SQLException;

    List<BorrowRecord> getPendingRequests() throws SQLException;

    List<BorrowRecord> getOverdueRecords() throws SQLException;

    List<BorrowRecord> getAllBorrows(int page, int pageSize) throws SQLException;

    int getBorrowCount() throws SQLException;

    int getActiveLoanCount() throws SQLException;

    BigDecimal getTotalFines() throws SQLException;

    List<Object[]> getMostBorrowedBooks(int limit) throws SQLException;

    int[] getOverdueCountsByMonth(int year) throws SQLException;

    int getBorrowedCopyCount() throws SQLException;

    List<BorrowRecord> getRecentBorrows(int limit) throws SQLException;

    boolean payFine(int borrowId) throws SQLException;

    boolean canBorrow(int userId) throws SQLException;
}
