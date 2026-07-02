package com.library.dao;

import com.library.dao.impl.*;

import com.library.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the simpler DAOs (Genre, Author, Publisher, Review, BookCopy)
 * plus non-enriching BookDAO and BorrowRecordDAO paths, using a mocked JDBC chain.
 */
class SimpleDaoTest {

    private <T> T daoWith(T dao, JdbcTestSupport.Mocks m) {
        JdbcTestSupport.inject(dao, m.dbConnection);
        return dao;
    }

    // ---------- GenreDAO ----------
    @Test
    void genreFindByIdAndFindAll() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        GenreDAO dao = daoWith(new GenreDAOImpl(), m);
        when(m.resultSet.getInt("genre_id")).thenReturn(1);
        when(m.resultSet.getString("name")).thenReturn("Fiction");

        when(m.resultSet.next()).thenReturn(true);
        Optional<Genre> g = dao.findById(1);
        assertTrue(g.isPresent());
        assertEquals("Fiction", g.get().getName());

        when(m.resultSet.next()).thenReturn(true, false);
        List<Genre> all = dao.findAll();
        assertEquals(1, all.size());
    }

    @Test
    void genreSaveUpdateDelete() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        GenreDAO dao = daoWith(new GenreDAOImpl(), m);
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("genre_id")).thenReturn(5);
        Genre g = new Genre();
        g.setName("Sci-Fi");
        assertEquals(5, dao.save(g).getGenreId());

        Genre g2 = new Genre();
        g2.setGenreId(5);
        g2.setName("SF");
        assertTrue(dao.update(g2));
        assertTrue(dao.deleteById(5));
    }

    // ---------- AuthorDAO ----------
    @Test
    void authorFindByIdAndSave() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        AuthorDAO dao = daoWith(new AuthorDAOImpl(), m);
        when(m.resultSet.getInt("author_id")).thenReturn(1);
        when(m.resultSet.getString("first_name")).thenReturn("George");
        when(m.resultSet.getString("last_name")).thenReturn("Orwell");
        when(m.resultSet.getString("nationality")).thenReturn("British");
        when(m.resultSet.getString("bio")).thenReturn("bio");

        when(m.resultSet.next()).thenReturn(true);
        Optional<Author> a = dao.findById(1);
        assertTrue(a.isPresent());
        assertEquals("George Orwell", a.get().getFullName());

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("author_id")).thenReturn(9);
        Author toSave = new Author();
        toSave.setFirstName("A");
        toSave.setLastName("B");
        assertEquals(9, dao.save(toSave).getAuthorId());
        assertTrue(dao.deleteById(9));
    }

    // ---------- PublisherDAO ----------
    @Test
    void publisherFindByIdAndUpdate() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        PublisherDAO dao = daoWith(new PublisherDAOImpl(), m);
        when(m.resultSet.getInt("publisher_id")).thenReturn(1);
        when(m.resultSet.getString("publisher_uuid")).thenReturn("u");
        when(m.resultSet.getString("name")).thenReturn("Penguin");
        when(m.resultSet.getString("address")).thenReturn("addr");
        when(m.resultSet.getString("website")).thenReturn("web");

        when(m.resultSet.next()).thenReturn(true);
        Optional<Publisher> p = dao.findById(1);
        assertTrue(p.isPresent());
        assertEquals("Penguin", p.get().getName());

        Publisher p2 = new Publisher();
        p2.setPublisherId(1);
        p2.setName("N");
        assertTrue(dao.update(p2));
    }

    // ---------- ReviewDAO ----------
    @Test
    void reviewFindByBookAndAverageAndCount() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        ReviewDAO dao = daoWith(new ReviewDAOImpl(), m);
        when(m.resultSet.getInt("review_id")).thenReturn(1);
        when(m.resultSet.getInt("user_id")).thenReturn(3);
        when(m.resultSet.getInt("book_id")).thenReturn(2);
        when(m.resultSet.getInt("rating")).thenReturn(5);
        when(m.resultSet.getString("comment")).thenReturn("great");
        when(m.resultSet.getDate("created_date")).thenReturn(Date.valueOf(LocalDate.now()));

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findByBookId(2).size());

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getDouble(1)).thenReturn(4.5);
        when(m.resultSet.wasNull()).thenReturn(false);
        assertEquals(4.5, dao.getAverageRating(2));

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt(1)).thenReturn(11);
        assertEquals(11, dao.count());

        when(m.resultSet.next()).thenReturn(true);
        assertTrue(dao.existsByUserAndBook(3, 2));
    }

    @Test
    void reviewSaveAndDelete() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        ReviewDAO dao = daoWith(new ReviewDAOImpl(), m);
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("review_id")).thenReturn(7);
        Review r = new Review();
        r.setUserId(3);
        r.setBookId(2);
        r.setRating(4);
        r.setComment("ok");
        assertEquals(7, dao.save(r).getReviewId());
        assertTrue(dao.deleteById(7));
    }

    // ---------- BookCopyDAO ----------
    @Test
    void bookCopyFindAvailableAndCounts() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BookCopyDAO dao = daoWith(new BookCopyDAOImpl(), m);
        when(m.resultSet.getInt("copy_id")).thenReturn(10);
        when(m.resultSet.getInt("book_id")).thenReturn(2);
        when(m.resultSet.getInt("copy_number")).thenReturn(1);
        when(m.resultSet.getString("condition")).thenReturn("GOOD");
        when(m.resultSet.getDate("acquisition_date")).thenReturn(Date.valueOf(LocalDate.now()));

        when(m.resultSet.next()).thenReturn(true);
        Optional<BookCopy> c = dao.findAvailableCopy(2);
        assertTrue(c.isPresent());
        assertEquals(BookCopy.Condition.GOOD, c.get().getCondition());

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findByBookId(2).size());

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt(1)).thenReturn(4);
        assertEquals(4, dao.countAll());
        assertEquals(4, dao.countByCondition(BookCopy.Condition.DAMAGED));
    }

    @Test
    void bookCopySaveUpdateDelete() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BookCopyDAO dao = daoWith(new BookCopyDAOImpl(), m);
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("copy_id")).thenReturn(20);
        BookCopy copy = new BookCopy();
        copy.setBookId(2);
        copy.setCopyNumber(3);
        assertEquals(20, dao.save(copy).getCopyId());

        BookCopy c2 = new BookCopy();
        c2.setCopyId(20);
        c2.setCondition(BookCopy.Condition.WORN);
        assertTrue(dao.update(c2));
        assertTrue(dao.deleteById(20));
    }

    // ---------- BookDAO (non-enriching paths) ----------
    @Test
    void bookDaoSaveUpdateDeleteAndCounts() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BookDAO dao = daoWith(new BookDAOImpl(), m);

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("book_id")).thenReturn(100);
        Book b = new Book();
        b.setTitle("T");
        b.setIsbn("I");
        b.setPublicationYear(2000);
        b.setTotalCopies(3);
        b.setAvailableCopies(3);
        assertEquals(100, dao.save(b).getBookId());

        Book b2 = new Book();
        b2.setBookId(100);
        b2.setTitle("T2");
        b2.setIsbn("I2");
        b2.setPublicationYear(2001);
        b2.setTotalCopies(4);
        assertTrue(dao.update(b2));
        assertTrue(dao.deleteById(100));
        assertTrue(dao.addCopies(100, 2));
        assertTrue(dao.incrementAvailableCopies(100));
        assertTrue(dao.decrementAvailableCopies(100));

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt(1)).thenReturn(50);
        assertEquals(50, dao.count());
        assertEquals(50, dao.countAvailable());
        assertEquals(50, dao.countByTitle("t"));
        assertEquals(50, dao.countSearch("t"));
        assertEquals(50, dao.countByGenre(1));
    }

    @Test
    void bookDaoFindAvailableAndSearchByTitleMapRows() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BookDAO dao = daoWith(new BookDAOImpl(), m);
        when(m.resultSet.getInt("book_id")).thenReturn(1);
        when(m.resultSet.getString("isbn")).thenReturn("isbn");
        when(m.resultSet.getString("title")).thenReturn("1984");
        when(m.resultSet.getInt("publication_year")).thenReturn(1949);
        when(m.resultSet.getInt("total_copies")).thenReturn(5);
        when(m.resultSet.getInt("available_copies")).thenReturn(3);
        when(m.resultSet.getInt("publisher_id")).thenReturn(0);
        when(m.resultSet.wasNull()).thenReturn(true);
        when(m.resultSet.getString("description")).thenReturn("desc");

        when(m.resultSet.next()).thenReturn(true, false);
        List<Book> avail = dao.findAvailable(10, 0);
        assertEquals(1, avail.size());
        assertEquals("1984", avail.get(0).getTitle());

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.searchByTitle("19", 10, 0).size());
    }

    // ---------- BorrowRecordDAO ----------
    @Test
    void borrowRecordFindByIdAndStatusMethods() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BorrowRecordDAO dao = daoWith(new BorrowRecordDAOImpl(), m);
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

        when(m.resultSet.next()).thenReturn(true);
        Optional<BorrowRecord> r = dao.findById(1);
        assertTrue(r.isPresent());
        assertEquals(BorrowRecord.Status.APPROVED, r.get().getStatus());

        when(m.resultSet.next()).thenReturn(true, false);
        assertEquals(1, dao.findByUserId(3).size());

        // status-changing updates return true (executeUpdate mocked to 1)
        assertTrue(dao.approve(1, 2));
        assertTrue(dao.reject(1));
        assertTrue(dao.requestReturn(1));
        assertTrue(dao.returnBook(1, BigDecimal.TEN));

        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt(1)).thenReturn(3);
        assertEquals(3, dao.count());
        assertEquals(3, dao.countActiveByUserId(3));
        assertEquals(3, dao.countActiveLoans());
        assertEquals(3, dao.countBorrowedCopies());

        when(m.resultSet.next()).thenReturn(true);
        assertTrue(dao.hasActiveBorrowForBook(3, 2));
        when(m.resultSet.next()).thenReturn(true);
        assertTrue(dao.hasActiveBorrowsForBook(2));
        when(m.resultSet.next()).thenReturn(true);
        assertTrue(dao.hasAnyBorrowsForBook(2));
    }

    @Test
    void borrowRecordSaveGeneratesId() throws Exception {
        JdbcTestSupport.Mocks m = JdbcTestSupport.newMocks();
        BorrowRecordDAO dao = daoWith(new BorrowRecordDAOImpl(), m);
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("borrow_id")).thenReturn(55);
        BorrowRecord r = new BorrowRecord();
        r.setUserId(3);
        r.setCopyId(10);
        r.setBorrowDate(LocalDate.now());
        r.setDueDate(LocalDate.now().plusDays(14));
        r.setStatus(BorrowRecord.Status.REQUESTED);
        r.setFineAmount(BigDecimal.ZERO);
        assertEquals(55, dao.save(r).getBorrowId());
    }
}
