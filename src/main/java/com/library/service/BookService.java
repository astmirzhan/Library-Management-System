package com.library.service;

import com.library.dao.AuthorDAO;
import com.library.dao.BookDAO;
import com.library.dao.GenreDAO;
import com.library.model.Author;
import com.library.model.Book;
import com.library.model.Genre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for book-related business logic.
 * Provides catalog browsing, search, and book management.
 */
public class BookService {

    private static final Logger logger = LogManager.getLogger(BookService.class);
    private final BookDAO bookDAO;
    private final AuthorDAO authorDAO;
    private final GenreDAO genreDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
        this.authorDAO = new AuthorDAO();
        this.genreDAO = new GenreDAO();
    }

    public BookService(BookDAO bookDAO, AuthorDAO authorDAO, GenreDAO genreDAO) {
        this.bookDAO = bookDAO;
        this.authorDAO = authorDAO;
        this.genreDAO = genreDAO;
    }

    /**
     * Returns books with pagination.
     *
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of books
     * @throws SQLException if database error occurs
     */
    public List<Book> getAllBooks(int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return bookDAO.findAll(pageSize, offset);
    }

    /**
     * Finds a book by ID with all related data (authors, genres).
     *
     * @param bookId the book ID
     * @return Optional containing the book with relations
     * @throws SQLException if database error occurs
     */
    public Optional<Book> getBookById(int bookId) throws SQLException {
        return bookDAO.findById(bookId);
    }

    /**
     * Searches books by query string (title or ISBN) with pagination.
     *
     * @param query    the search query
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of matching books
     * @throws SQLException if database error occurs
     */
    public List<Book> searchBooks(String query, int page, int pageSize) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks(page, pageSize);
        }
        int offset = (page - 1) * pageSize;
        return bookDAO.search(query.trim(), pageSize, offset);
    }

    /**
     * Filters books by genre with pagination.
     *
     * @param genreId  the genre ID
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of books in the genre
     * @throws SQLException if database error occurs
     */
    public List<Book> getBooksByGenre(int genreId, int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return bookDAO.findByGenre(genreId, pageSize, offset);
    }

    /**
     * Returns only available books (with copies > 0) with pagination.
     *
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of available books
     * @throws SQLException if database error occurs
     */
    public List<Book> getAvailableBooks(int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return bookDAO.findAvailable(pageSize, offset);
    }

    /**
     * Adds a new book to the catalog with authors and genres.
     *
     * @param book      the book to add
     * @param authorIds list of author IDs
     * @param genreIds  list of genre IDs
     * @return the saved book
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if validation fails
     */
    public Book addBook(Book book, List<Integer> authorIds, List<Integer> genreIds)
            throws SQLException {
        validateBook(book);

        Book saved = bookDAO.save(book);

        if (authorIds != null) {
            for (Integer authorId : authorIds) {
                bookDAO.addAuthorToBook(saved.getBookId(), authorId);
            }
        }

        if (genreIds != null) {
            for (Integer genreId : genreIds) {
                bookDAO.addGenreToBook(saved.getBookId(), genreId);
            }
        }

        logger.info("Book added with authors and genres: {}", saved.getTitle());
        return saved;
    }

    /**
     * Updates an existing book.
     *
     * @param book the book to update
     * @return true if updated successfully
     * @throws SQLException if database error occurs
     */
    public boolean updateBook(Book book) throws SQLException {
        validateBook(book);
        return bookDAO.update(book);
    }

    /**
     * Deletes a book by ID.
     *
     * @param bookId the book ID
     * @return true if deleted successfully
     * @throws SQLException if database error occurs
     */
    public boolean deleteBook(int bookId) throws SQLException {
        return bookDAO.deleteById(bookId);
    }

    /**
     * Returns total count of books (for pagination).
     *
     * @return book count
     * @throws SQLException if database error occurs
     */
    public int getBookCount() throws SQLException {
        return bookDAO.count();
    }

    /**
     * Returns all authors (for filters).
     *
     * @return list of all authors
     * @throws SQLException if database error occurs
     */
    public List<Author> getAllAuthors() throws SQLException {
        return authorDAO.findAll();
    }

    /**
     * Returns all genres (for filters).
     *
     * @return list of all genres
     * @throws SQLException if database error occurs
     */
    public List<Genre> getAllGenres() throws SQLException {
        return genreDAO.findAll();
    }

    /**
     * Validates book fields.
     */
    private void validateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN cannot be empty");
        }
        if (book.getPublicationYear() < 1000 || book.getPublicationYear() > 2100) {
            throw new IllegalArgumentException("Invalid publication year");
        }
        if (book.getTotalCopies() < 0) {
            throw new IllegalArgumentException("Total copies cannot be negative");
        }
    }
}