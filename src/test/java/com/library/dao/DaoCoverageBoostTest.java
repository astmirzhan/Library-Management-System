package com.library.dao;

import com.library.dao.impl.*;

import com.library.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Additional DAO tests covering list/update/delete paths to push per-class
 * coverage of the weaker DAOs above the 50% threshold.
 */
class DaoCoverageBoostTest {

    private <T> T inject(T dao, JdbcTestSupport.Mocks m) {
        JdbcTestSupport.inject(dao, m.dbConnection);
        return dao;
    }

    @Test
    void publisherFindAllSaveDelete() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        PublisherDAO dao = inject(new PublisherDAOImpl(), m);
        when(m.resultSet.getInt("publisher_id")).thenReturn(1);
        when(m.resultSet.getString("publisher_uuid")).thenReturn("u");
        when(m.resultSet.getString("name")).thenReturn("Penguin");
        when(m.resultSet.getString("address")).thenReturn("a");
        when(m.resultSet.getString("website")).thenReturn("w");

        when(m.resultSet.next()).thenReturn(true, false);
        List<Publisher> all = dao.findAll();
        assertEquals(1, all.size());

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("publisher_id")).thenReturn(9);
        Publisher p = new Publisher();
        p.setName("New");
        assertEquals(9, dao.save(p).getPublisherId());
        assertTrue(dao.deleteById(9));
    }

    @Test
    void authorFindAllFindByBookIdUpdate() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        AuthorDAO dao = inject(new AuthorDAOImpl(), m);
        when(m.resultSet.getInt("author_id")).thenReturn(1);
        when(m.resultSet.getString("first_name")).thenReturn("George");
        when(m.resultSet.getString("last_name")).thenReturn("Orwell");
        when(m.resultSet.getString("nationality")).thenReturn("British");
        when(m.resultSet.getString("bio")).thenReturn("bio");

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findAll().size());

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findByBookId(2).size());

        Author a = new Author();
        a.setAuthorId(1);
        a.setFirstName("A");
        a.setLastName("B");
        assertTrue(dao.update(a));
    }

    @Test
    void genreFindByBookIdAndUpdate() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        GenreDAO dao = inject(new GenreDAOImpl(), m);
        when(m.resultSet.getInt("genre_id")).thenReturn(1);
        when(m.resultSet.getString("name")).thenReturn("Fiction");
        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findByBookId(2).size());
    }

    @Test
    void borrowRecordListQueriesAndUpdateDelete() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BorrowRecordDAO dao = inject(new BorrowRecordDAOImpl(), m);
        when(m.resultSet.getInt("borrow_id")).thenReturn(1);
        when(m.resultSet.getInt("user_id")).thenReturn(3);
        when(m.resultSet.getInt("copy_id")).thenReturn(10);
        when(m.resultSet.getDate("borrow_date")).thenReturn(Date.valueOf(LocalDate.now()));
        when(m.resultSet.getDate("due_date")).thenReturn(Date.valueOf(LocalDate.now().plusDays(14)));
        when(m.resultSet.getDate("return_date")).thenReturn(null);
        when(m.resultSet.getString("status")).thenReturn("APPROVED");
        when(m.resultSet.getBigDecimal("fine_amount")).thenReturn(BigDecimal.ZERO);
        when(m.resultSet.getBoolean("fine_paid")).thenReturn(false);
        when(m.resultSet.getInt("approved_by")).thenReturn(2);
        when(m.resultSet.getDate("approval_date")).thenReturn(Date.valueOf(LocalDate.now()));
        when(m.resultSet.wasNull()).thenReturn(false);

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findAll(10, 0).size());
        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findActiveByUserId(3).size());
        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findPendingRequests().size());
        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findOverdue().size());
        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findReturnRequested().size());

        BorrowRecord rec = new BorrowRecord();
        rec.setBorrowId(1);
        rec.setDueDate(LocalDate.now());
        rec.setStatus(BorrowRecord.Status.APPROVED);
        rec.setFineAmount(BigDecimal.ZERO);
        assertTrue(dao.update(rec));
        assertTrue(dao.deleteById(1));
    }

    @Test
    void bookCopyFindAllAndFindById() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BookCopyDAO dao = inject(new BookCopyDAOImpl(), m);
        when(m.resultSet.getInt("copy_id")).thenReturn(1);
        when(m.resultSet.getInt("book_id")).thenReturn(2);
        when(m.resultSet.getInt("copy_number")).thenReturn(1);
        when(m.resultSet.getString("condition")).thenReturn("GOOD");
        when(m.resultSet.getDate("acquisition_date")).thenReturn(Date.valueOf(LocalDate.now()));

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findAll().size());
        when(m.resultSet.next()).thenReturn(true);
        assertTrue(dao.findById(1).isPresent());
    }
}
