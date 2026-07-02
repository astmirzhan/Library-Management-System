package com.library.service;

import com.library.dao.AuthorDAO;
import com.library.dao.BookCopyDAO;
import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.GenreDAO;
import com.library.model.Author;
import com.library.model.Book;
import com.library.model.BookCopy;
import com.library.model.Genre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for book-related business logic.
 * Provides catalog browsing, search, and book management.
 */
@Service
public class BookService {

    private static final Logger logger = LogManager.getLogger(BookService.class);
    private static final int MAX_COPIES = 100;
    private final BookDAO bookDAO;
    private final AuthorDAO authorDAO;
    private final GenreDAO genreDAO;
    private final BookCopyDAO bookCopyDAO;
    private final BorrowRecordDAO borrowDAO;

    @Autowired
    public BookService(BookDAO bookDAO, AuthorDAO authorDAO, GenreDAO genreDAO,
                       BookCopyDAO bookCopyDAO, BorrowRecordDAO borrowDAO) {
        this.bookDAO = bookDAO;
        this.authorDAO = authorDAO;
        this.genreDAO = genreDAO;
        this.bookCopyDAO = bookCopyDAO;
        this.borrowDAO = borrowDAO;
    }

    /**
     * Creates a book with the given number of physical copies (librarian flow).
     * Sets total/available copies to the requested count and inserts copy rows.
     *
     * @param book      the book to create
     * @param authorIds author IDs to link (may be null)
     * @param genreIds  genre IDs to link (may be null)
     * @param copies    number of physical copies to create
     * @return the saved book
     * @throws SQLException if database error occurs
     */
    public Book createBook(Book book, List<Integer> authorIds, List<Integer> genreIds, int copies)
            throws SQLException {
        if (copies < 1 || copies > MAX_COPIES) {
            throw new IllegalArgumentException("Number of copies must be between 1 and " + MAX_COPIES);
        }
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);

        Book saved = addBook(book, authorIds, genreIds);   // validate + save + link

        for (int i = 1; i <= copies; i++) {
            BookCopy copy = new BookCopy();
            copy.setBookId(saved.getBookId());
            copy.setCopyNumber(i);
            copy.setCondition(BookCopy.Condition.GOOD);
            bookCopyDAO.save(copy);
        }
        logger.info("Book created with {} copies: {}", copies, saved.getTitle());
        return saved;
    }

    /**
     * Adds more physical copies to an existing book and bumps its copy counters.
     *
     * @param bookId the book ID
     * @param count  number of copies to add
     * @throws SQLException if database error occurs
     */
    public void addCopies(int bookId, int count) throws SQLException {
        if (count < 1 || count > MAX_COPIES) {
            throw new IllegalArgumentException("Number of copies must be between 1 and " + MAX_COPIES);
        }
        int start = bookCopyDAO.findByBookId(bookId).size();
        for (int i = 1; i <= count; i++) {
            BookCopy copy = new BookCopy();
            copy.setBookId(bookId);
            copy.setCopyNumber(start + i);
            copy.setCondition(BookCopy.Condition.GOOD);
            bookCopyDAO.save(copy);
        }
        bookDAO.addCopies(bookId, count);
        logger.info("Added {} copies to book {}", count, bookId);
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
    /**
     * Searches books strictly by title with pagination (librarian inventory search).
     *
     * @param query    title fragment
     * @param page     page number (1-indexed)
     * @param pageSize page size
     * @return list of matching books
     * @throws SQLException if database error occurs
     */
    public List<Book> searchBooksByTitle(String query, int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        return bookDAO.searchByTitle(query.trim(), pageSize, offset);
    }

    /**
     * Counts books whose title matches the query.
     */
    public int getTitleSearchCount(String query) throws SQLException {
        return bookDAO.countByTitle(query.trim());
    }

    /**
     * Returns all books by a given author, enriched with authors and genres
     * (for the catalog author filter).
     *
     * @param authorId the author ID
     * @return list of the author's books
     * @throws SQLException if database error occurs
     */
    public List<Book> getBooksByAuthor(int authorId) throws SQLException {
        List<Book> books = bookDAO.findByAuthor(authorId);
        for (Book book : books) {
            book.setAuthors(authorDAO.findByBookId(book.getBookId()));
            book.setGenres(genreDAO.findByBookId(book.getBookId()));
        }
        return books;
    }

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
     * Updates the editable fields (title, ISBN, year, description) of an existing book,
     * preserving copy counts.
     *
     * @param bookId      the book ID
     * @param title       new title
     * @param isbn        new ISBN
     * @param year        new publication year
     * @param description new description (may be null)
     * @return true if updated
     * @throws SQLException             if database error occurs
     * @throws IllegalArgumentException if book not found or validation fails
     */
    public boolean editBook(int bookId, String title, String isbn, int year, String description)
            throws SQLException {
        Book book = bookDAO.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublicationYear(year);
        book.setDescription(description);
        return updateBook(book);   // validateBook + persist
    }

    /**
     * Deletes a book by ID.
     *
     * @param bookId the book ID
     * @return true if deleted successfully
     * @throws SQLException if database error occurs
     */
    public boolean deleteBook(int bookId) throws SQLException {
        if (borrowDAO.hasActiveBorrowsForBook(bookId)) {
            throw new IllegalStateException(
                    "Cannot delete: this book has active borrow requests or loans");
        }
        if (borrowDAO.hasAnyBorrowsForBook(bookId)) {
            throw new IllegalStateException(
                    "Cannot delete: this book has borrowing history");
        }
        // No borrow history — safe to delete (copies are removed via ON DELETE CASCADE).
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
     * Returns the total number of physical copies.
     */
    public int getCopyCount() throws SQLException {
        return bookCopyDAO.countAll();
    }

    /**
     * Returns the number of copies in the given condition.
     */
    public int getCopyCountByCondition(com.library.model.BookCopy.Condition condition) throws SQLException {
        return bookCopyDAO.countByCondition(condition);
    }

    /**
     * Returns the count of books matching the same filter the catalog applied,
     * so pagination reflects the filtered result set (not the whole catalog).
     *
     * @param query        search query (title/ISBN), or null/blank
     * @param genreId      genre filter, or null
     * @param availability "available" to restrict to in-stock books, else null
     * @return matching book count
     * @throws SQLException if database error occurs
     */
    public int getFilteredBookCount(String query, Integer genreId, String availability)
            throws SQLException {
        if (query != null && !query.trim().isEmpty()) {
            return bookDAO.countSearch(query.trim());
        }
        if (genreId != null) {
            return bookDAO.countByGenre(genreId);
        }
        if ("available".equalsIgnoreCase(availability)) {
            return bookDAO.countAvailable();
        }
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