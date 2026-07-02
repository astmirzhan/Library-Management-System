package com.library.service;

import com.library.service.impl.UserServiceImpl;
import com.library.service.impl.BookServiceImpl;
import com.library.service.impl.BorrowServiceImpl;
import com.library.service.impl.ReviewServiceImpl;

import com.library.dao.*;
import com.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class BookServiceTest {

    @Mock BookDAO bookDAO;
    @Mock AuthorDAO authorDAO;
    @Mock GenreDAO genreDAO;
    @Mock BookCopyDAO bookCopyDAO;
    @Mock BorrowRecordDAO borrowDAO;

    private BookService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BookServiceImpl(bookDAO, authorDAO, genreDAO, bookCopyDAO, borrowDAO);
    }

    private Book validBook() {
        Book b = new Book();
        b.setTitle("Title");
        b.setIsbn("ISBN");
        b.setPublicationYear(2000);
        b.setTotalCopies(3);
        return b;
    }

    // ---------- validateBook (via addBook) ----------
    @Test
    void addBookRejectsEmptyTitle() {
        Book b = validBook();
        b.setTitle(" ");
        assertThrows(IllegalArgumentException.class, () -> service.addBook(b, null, null));
    }

    @Test
    void addBookRejectsBadYear() {
        Book b = validBook();
        b.setPublicationYear(999);
        assertThrows(IllegalArgumentException.class, () -> service.addBook(b, null, null));
    }

    @Test
    void addBookSavesAndLinks() throws Exception {
        Book b = validBook();
        when(bookDAO.save(any())).thenAnswer(inv -> {
            Book bb = inv.getArgument(0);
            bb.setBookId(7);
            return bb;
        });
        service.addBook(b, java.util.List.of(1), java.util.List.of(2));
        verify(bookDAO).addAuthorToBook(7, 1);
        verify(bookDAO).addGenreToBook(7, 2);
    }

    // ---------- createBook (copies) ----------
    @Test
    void createBookRejectsZeroCopies() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBook(validBook(), null, null, 0));
    }

    @Test
    void createBookRejectsTooManyCopies() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBook(validBook(), null, null, 101));
    }

    @Test
    void createBookInsertsCopies() throws Exception {
        when(bookDAO.save(any())).thenAnswer(inv -> {
            Book bb = inv.getArgument(0);
            bb.setBookId(7);
            return bb;
        });
        service.createBook(validBook(), null, null, 3);
        verify(bookCopyDAO, times(3)).save(any());
    }

    // ---------- editBook ----------
    @Test
    void editBookThrowsWhenNotFound() throws Exception {
        when(bookDAO.findById(9)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.editBook(9, "T", "I", 2000, "d"));
    }

    @Test
    void editBookUpdatesExisting() throws Exception {
        Book existing = validBook();
        existing.setBookId(9);
        when(bookDAO.findById(9)).thenReturn(Optional.of(existing));
        when(bookDAO.update(any())).thenReturn(true);
        assertTrue(service.editBook(9, "New", "ISBN2", 2010, "desc"));
    }

    // ---------- deleteBook (guards) ----------
    @Test
    void deleteBookBlockedByActiveBorrows() throws Exception {
        when(borrowDAO.hasActiveBorrowsForBook(5)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.deleteBook(5));
    }

    @Test
    void deleteBookBlockedByHistory() throws Exception {
        when(borrowDAO.hasActiveBorrowsForBook(5)).thenReturn(false);
        when(borrowDAO.hasAnyBorrowsForBook(5)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.deleteBook(5));
    }

    @Test
    void deleteBookAllowedWhenNoHistory() throws Exception {
        when(borrowDAO.hasActiveBorrowsForBook(5)).thenReturn(false);
        when(borrowDAO.hasAnyBorrowsForBook(5)).thenReturn(false);
        when(bookDAO.deleteById(5)).thenReturn(true);
        assertTrue(service.deleteBook(5));
    }

    // ---------- pagination / filtered count ----------
    @Test
    void getAllBooksComputesOffset() throws Exception {
        service.getAllBooks(3, 10);
        verify(bookDAO).findAll(10, 20);
    }

    @Test
    void filteredCountUsesSearchWhenQueryPresent() throws Exception {
        when(bookDAO.countSearch("dune")).thenReturn(4);
        assertEquals(4, service.getFilteredBookCount("dune", null, null));
    }

    @Test
    void filteredCountUsesGenreWhenGenrePresent() throws Exception {
        when(bookDAO.countByGenre(2)).thenReturn(6);
        assertEquals(6, service.getFilteredBookCount(null, 2, null));
    }

    @Test
    void addCopiesRejectsInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> service.addCopies(1, 0));
    }
}
