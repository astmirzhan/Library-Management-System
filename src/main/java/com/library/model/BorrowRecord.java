package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a borrowing transaction between a user and a book copy.
 * Tracks the lifecycle from request to return, including fines for overdue books.
 */
public class BorrowRecord {

    public enum Status {
        REQUESTED, APPROVED, RETURNED, RETURN_REQUESTED, REJECTED
    }

    private int borrowId;
    private int userId;
    private int copyId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Status status;
    private BigDecimal fineAmount;
    private boolean finePaid;
    private Integer approvedBy;
    private LocalDate approvalDate;

    private User user;
    private Book book;
    private Integer copyNumber;

    public BorrowRecord() {
        this.fineAmount = BigDecimal.ZERO;
        this.finePaid = false;
        this.status = Status.REQUESTED;
    }

    public BorrowRecord(int borrowId, int userId, int copyId, LocalDate borrowDate,
                        LocalDate dueDate, LocalDate returnDate, Status status,
                        BigDecimal fineAmount, boolean finePaid, Integer approvedBy,
                        LocalDate approvalDate) {
        this.borrowId = borrowId;
        this.userId = userId;
        this.copyId = copyId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.fineAmount = fineAmount;
        this.finePaid = finePaid;
        this.approvedBy = approvedBy;
        this.approvalDate = approvalDate;
    }

    public int getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(int borrowId) {
        this.borrowId = borrowId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCopyId() {
        return copyId;
    }

    public void setCopyId(int copyId) {
        this.copyId = copyId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Integer getCopyNumber() {
        return copyNumber;
    }

    public void setCopyNumber(Integer copyNumber) {
        this.copyNumber = copyNumber;
    }

    public boolean isActive() {
        return returnDate == null && status == Status.APPROVED;
    }

    public boolean isOverdue() {
        return isActive() && LocalDate.now().isAfter(dueDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowRecord that = (BorrowRecord) o;
        return borrowId == that.borrowId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(borrowId);
    }

    @Override
    public String toString() {
        return "BorrowRecord{" +
                "borrowId=" + borrowId +
                ", userId=" + userId +
                ", copyId=" + copyId +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", status=" + status +
                '}';
    }
}
