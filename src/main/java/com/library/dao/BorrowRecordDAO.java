package com.library.dao;

import com.library.model.BorrowRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** DAO contract for the BORROW_RECORD table. */
public interface BorrowRecordDAO extends BaseDAO<BorrowRecord, Integer> {
    int count() throws SQLException;
    List<BorrowRecord> findAll(int limit, int offset) throws SQLException;
    List<BorrowRecord> findByUserId(int userId) throws SQLException;
    boolean hasUserBorrowedBook(int userId, int bookId) throws SQLException;
    boolean hasActiveBorrowForBook(int userId, int bookId) throws SQLException;
    List<BorrowRecord> findActiveByUserId(int userId) throws SQLException;
    List<BorrowRecord> findPendingRequests() throws SQLException;
    List<BorrowRecord> findOverdue() throws SQLException;
    int countActiveLoans() throws SQLException;
    int countBorrowedCopies() throws SQLException;
    List<BorrowRecord> findReturnRequested() throws SQLException;
    int countActiveByUserId(int userId) throws SQLException;
    boolean hasActiveBorrowsForBook(int bookId) throws SQLException;
    BigDecimal totalFines() throws SQLException;
    List<Object[]> mostBorrowedBooks(int limit) throws SQLException;
    int[] overdueCountsByMonth(int year) throws SQLException;
    boolean hasAnyBorrowsForBook(int bookId) throws SQLException;
    boolean reject(int borrowId) throws SQLException;
    boolean requestReturn(int borrowId) throws SQLException;
    boolean approve(int borrowId, int approvedBy) throws SQLException;
    boolean returnBook(int borrowId, BigDecimal fineAmount) throws SQLException;
}
