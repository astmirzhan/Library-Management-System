package com.library.service;

import com.library.service.impl.UserServiceImpl;
import com.library.service.impl.BookServiceImpl;
import com.library.service.impl.BorrowServiceImpl;
import com.library.service.impl.ReviewServiceImpl;

import com.library.dao.BookCopyDAO;
import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.UserDAO;
import com.library.model.BookCopy;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.util.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class BorrowServiceTest {

    @Mock BorrowRecordDAO borrowDAO;
    @Mock BookCopyDAO copyDAO;
    @Mock BookDAO bookDAO;
    @Mock UserDAO userDAO;
    @Mock ConfigLoader config;

    private BorrowService service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new BorrowServiceImpl(borrowDAO, copyDAO, bookDAO, userDAO, config);
    }

    private User reader() {
        User u = new User();
        u.setUserId(3);
        u.setRole(User.Role.READER);
        return u;
    }

    // ---------- calculateFine (boundary cases) ----------
    @Test
    void calculateFineZeroWhenNotOverdue() {
        assertEquals(BigDecimal.ZERO,
                service.calculateFine(LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @Test
    void calculateFineZeroWhenNullDates() {
        assertEquals(BigDecimal.ZERO, service.calculateFine(null, LocalDate.now()));
    }

    @Test
    void calculateFineComputesOverdueAmount() {
        when(config.getDailyFineAmount()).thenReturn(1500.0);
        BigDecimal fine = service.calculateFine(LocalDate.now().minusDays(3), LocalDate.now());
        assertEquals(0, fine.compareTo(new BigDecimal("4500.00")));
    }

    // ---------- requestBorrow ----------
    @Test
    void requestBorrowThrowsWhenUserMissing() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().plusDays(7)));
    }

    @Test
    void requestBorrowThrowsWhenNotReader() throws Exception {
        User librarian = new User();
        librarian.setUserId(3);
        librarian.setRole(User.Role.LIBRARIAN);
        when(userDAO.findById(3)).thenReturn(Optional.of(librarian));
        assertThrows(IllegalStateException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().plusDays(7)));
    }

    @Test
    void requestBorrowThrowsWhenDueDateInPast() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        assertThrows(IllegalArgumentException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().minusDays(1)));
    }

    @Test
    void requestBorrowThrowsWhenAlreadyBorrowed() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        when(borrowDAO.hasActiveBorrowForBook(3, 1)).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().plusDays(7)));
    }

    @Test
    void requestBorrowThrowsWhenLimitReached() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        when(borrowDAO.hasActiveBorrowForBook(3, 1)).thenReturn(false);
        when(config.getMaxBorrowingLimit()).thenReturn(5);
        when(borrowDAO.countActiveByUserId(3)).thenReturn(5);
        assertThrows(IllegalStateException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().plusDays(7)));
    }

    @Test
    void requestBorrowThrowsWhenNoCopy() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        when(borrowDAO.hasActiveBorrowForBook(3, 1)).thenReturn(false);
        when(config.getMaxBorrowingLimit()).thenReturn(5);
        when(borrowDAO.countActiveByUserId(3)).thenReturn(0);
        when(copyDAO.findAvailableCopy(1)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,
                () -> service.requestBorrow(3, 1, LocalDate.now().plusDays(7)));
    }

    @Test
    void requestBorrowHappyPathSavesAndDecrements() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        when(borrowDAO.hasActiveBorrowForBook(3, 1)).thenReturn(false);
        when(config.getMaxBorrowingLimit()).thenReturn(5);
        when(borrowDAO.countActiveByUserId(3)).thenReturn(0);
        BookCopy copy = new BookCopy();
        copy.setCopyId(10);
        when(copyDAO.findAvailableCopy(1)).thenReturn(Optional.of(copy));
        when(borrowDAO.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BorrowRecord r = service.requestBorrow(3, 1, LocalDate.now().plusDays(7));
        assertEquals(BorrowRecord.Status.REQUESTED, r.getStatus());
        verify(bookDAO).decrementAvailableCopies(1);
    }

    // ---------- approveBorrow ----------
    @Test
    void approveBorrowRejectsNonLibrarian() throws Exception {
        when(userDAO.findById(3)).thenReturn(Optional.of(reader()));
        assertThrows(IllegalArgumentException.class, () -> service.approveBorrow(1, 3));
    }

    @Test
    void approveBorrowAllowsLibrarian() throws Exception {
        User lib = new User();
        lib.setUserId(2);
        lib.setRole(User.Role.LIBRARIAN);
        when(userDAO.findById(2)).thenReturn(Optional.of(lib));
        when(borrowDAO.approve(1, 2)).thenReturn(true);
        assertTrue(service.approveBorrow(1, 2));
    }

    // ---------- rejectBorrow ----------
    @Test
    void rejectBorrowFreesCopyOnSuccess() throws Exception {
        BorrowRecord rec = new BorrowRecord();
        rec.setCopyId(10);
        rec.setStatus(BorrowRecord.Status.REQUESTED);
        when(borrowDAO.findById(1)).thenReturn(Optional.of(rec));
        when(borrowDAO.reject(1)).thenReturn(true);
        BookCopy copy = new BookCopy();
        copy.setBookId(2);
        when(copyDAO.findById(10)).thenReturn(Optional.of(copy));

        assertTrue(service.rejectBorrow(1));
        verify(bookDAO).incrementAvailableCopies(2);
    }

    @Test
    void rejectBorrowThrowsWhenNotPending() throws Exception {
        BorrowRecord rec = new BorrowRecord();
        rec.setStatus(BorrowRecord.Status.APPROVED);
        when(borrowDAO.findById(1)).thenReturn(Optional.of(rec));
        assertThrows(IllegalStateException.class, () -> service.rejectBorrow(1));
    }

    // ---------- confirmReturn ----------
    @Test
    void confirmReturnThrowsWhenAlreadyReturned() throws Exception {
        BorrowRecord rec = new BorrowRecord();
        rec.setReturnDate(LocalDate.now());
        when(borrowDAO.findById(1)).thenReturn(Optional.of(rec));
        assertThrows(IllegalStateException.class,
                () -> service.confirmReturn(1, BookCopy.Condition.GOOD));
    }

    @Test
    void confirmReturnUpdatesStatusAndCopyCondition() throws Exception {
        BorrowRecord rec = new BorrowRecord();
        rec.setCopyId(10);
        rec.setDueDate(LocalDate.now());
        rec.setStatus(BorrowRecord.Status.APPROVED);
        when(borrowDAO.findById(1)).thenReturn(Optional.of(rec));
        BookCopy copy = new BookCopy();
        copy.setBookId(2);
        when(copyDAO.findById(10)).thenReturn(Optional.of(copy));

        BorrowRecord result = service.confirmReturn(1, BookCopy.Condition.WORN);
        assertEquals(BorrowRecord.Status.RETURNED, result.getStatus());
        assertNotNull(result.getReturnDate());
        verify(copyDAO).update(any(BookCopy.class));
        verify(bookDAO).incrementAvailableCopies(2);
    }

    // ---------- canBorrow ----------
    @Test
    void canBorrowReflectsLimit() throws Exception {
        when(config.getMaxBorrowingLimit()).thenReturn(5);
        when(borrowDAO.countActiveByUserId(3)).thenReturn(4);
        assertTrue(service.canBorrow(3));
        when(borrowDAO.countActiveByUserId(3)).thenReturn(5);
        assertFalse(service.canBorrow(3));
    }
}
