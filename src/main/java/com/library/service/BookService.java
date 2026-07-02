package com.library.service;

import com.library.model.Author;
import com.library.model.Book;
import com.library.model.BookCopy;
import com.library.model.Genre;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service contract for book-related business logic:
 * catalog browsing, search, and inventory management.
 */
public interface BookService {

    Book createBook(Book book, List<Integer> authorIds, List<Integer> genreIds, int copies) throws SQLException;

    void addCopies(int bookId, int count) throws SQLException;

    List<Book> getAllBooks(int page, int pageSize) throws SQLException;

    Optional<Book> getBookById(int bookId) throws SQLException;

    List<Book> searchBooks(String query, int page, int pageSize) throws SQLException;

    List<Book> searchBooksByTitle(String query, int page, int pageSize) throws SQLException;

    int getTitleSearchCount(String query) throws SQLException;

    List<Book> getBooksByAuthor(int authorId) throws SQLException;

    List<Book> getBooksByGenre(int genreId, int page, int pageSize) throws SQLException;

    List<Book> getAvailableBooks(int page, int pageSize) throws SQLException;

    Book addBook(Book book, List<Integer> authorIds, List<Integer> genreIds) throws SQLException;

    boolean updateBook(Book book) throws SQLException;

    boolean editBook(int bookId, String title, String isbn, int year, String description) throws SQLException;

    boolean deleteBook(int bookId) throws SQLException;

    int getBookCount() throws SQLException;

    int getCopyCount() throws SQLException;

    int getCopyCountByCondition(BookCopy.Condition condition) throws SQLException;

    int getFilteredBookCount(String query, Integer genreId, String availability) throws SQLException;

    List<Author> getAllAuthors() throws SQLException;

    List<Genre> getAllGenres() throws SQLException;
}
