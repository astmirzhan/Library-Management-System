package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.Author;
import com.library.model.Book;
import com.library.model.Genre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the BOOK table.
 * Provides CRUD operations, search, filtering and pagination for books.
 */
@Repository
public class BookDAOImpl implements BookDAO {

    private static final Logger logger = LogManager.getLogger(BookDAOImpl.class);
    private static final int DEFAULT_LIMIT = 100;
    private final DatabaseConnection dbConnection;
    private final AuthorDAO authorDAO;
    private final GenreDAO genreDAO;

    public BookDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.authorDAO = new AuthorDAOImpl();
        this.genreDAO = new GenreDAOImpl();
    }

    @Override
    public Book save(Book book) throws SQLException {
        String sql = "INSERT INTO book (isbn, title, publication_year, " +
                "total_copies, available_copies, publisher_id, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING book_id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setInt(3, book.getPublicationYear());
            stmt.setInt(4, book.getTotalCopies());
            stmt.setInt(5, book.getAvailableCopies());

            if (book.getPublisherId() != null) {
                stmt.setInt(6, book.getPublisherId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setString(7, book.getDescription());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    book.setBookId(rs.getInt("book_id"));
                    logger.info("Book saved: id={}, title={}", book.getBookId(), book.getTitle());
                    return book;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save book: {}", book.getTitle(), e);
            throw e;
        }
        throw new SQLException("Failed to save book");
    }

    @Override
    public Optional<Book> findById(Integer bookId) throws SQLException {
        String sql = "SELECT * FROM book WHERE book_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Book book = mapResultSet(rs);
                    book.setAuthors(authorDAO.findByBookId(bookId));
                    book.setGenres(genreDAO.findByBookId(bookId));
                    return Optional.of(book);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find book by id: {}", bookId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() throws SQLException {
        return findAll(DEFAULT_LIMIT, 0);
    }

    /**
     * Returns books with pagination.
     *
     * @param limit  max records per page
     * @param offset records to skip
     * @return list of books
     * @throws SQLException if database error occurs
     */
    public List<Book> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM book ORDER BY title LIMIT ? OFFSET ?";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Book book = mapResultSet(rs);
                    book.setAuthors(authorDAO.findByBookId(book.getBookId()));
                    book.setGenres(genreDAO.findByBookId(book.getBookId()));
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find books with pagination", e);
            throw e;
        }
        return books;
    }

    /**
     * Searches books by title or ISBN with pagination.
     *
     * @param query  the search query
     * @param limit  max records per page
     * @param offset records to skip
     * @return list of matching books
     * @throws SQLException if database error occurs
     */
    public List<Book> search(String query, int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM book WHERE LOWER(title) LIKE LOWER(?) OR isbn LIKE ? " +
                "ORDER BY title LIMIT ? OFFSET ?";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Book book = mapResultSet(rs);
                    book.setAuthors(authorDAO.findByBookId(book.getBookId()));
                    book.setGenres(genreDAO.findByBookId(book.getBookId()));
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to search books with query: {}", query, e);
            throw e;
        }
        return books;
    }

    /**
     * Finds books by genre with pagination.
     *
     * @param genreId the genre ID
     * @param limit   max records per page
     * @param offset  records to skip
     * @return list of books in the genre
     * @throws SQLException if database error occurs
     */
    public List<Book> findByGenre(int genreId, int limit, int offset) throws SQLException {
        String sql = "SELECT b.* FROM book b " +
                "JOIN book_genre bg ON b.book_id = bg.book_id " +
                "WHERE bg.genre_id = ? " +
                "ORDER BY b.title LIMIT ? OFFSET ?";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, genreId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Book book = mapResultSet(rs);
                    book.setAuthors(authorDAO.findByBookId(book.getBookId()));
                    book.setGenres(genreDAO.findByBookId(book.getBookId()));
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find books by genre: {}", genreId, e);
            throw e;
        }
        return books;
    }

    /**
     * Finds books by author.
     *
     * @param authorId the author ID
     * @return list of books by the author
     * @throws SQLException if database error occurs
     */
    public List<Book> findByAuthor(int authorId) throws SQLException {
        String sql = "SELECT b.* FROM book b " +
                "JOIN book_author ba ON b.book_id = ba.book_id " +
                "WHERE ba.author_id = ? ORDER BY b.title";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, authorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find books by author: {}", authorId, e);
            throw e;
        }
        return books;
    }

    /**
     * Returns only available books (with copies > 0).
     *
     * @param limit  max records per page
     * @param offset records to skip
     * @return list of available books
     * @throws SQLException if database error occurs
     */
    public List<Book> findAvailable(int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM book WHERE available_copies > 0 ORDER BY title LIMIT ? OFFSET ?";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find available books", e);
            throw e;
        }
        return books;
    }

    /**
     * Links a book with an author (book_author junction).
     *
     * @param bookId   the book ID
     * @param authorId the author ID
     * @throws SQLException if database error occurs
     */
    public void addAuthorToBook(int bookId, int authorId) throws SQLException {
        String sql = "INSERT INTO book_author (book_id, author_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            stmt.setInt(2, authorId);
            stmt.executeUpdate();
            logger.info("Author {} linked to book {}", authorId, bookId);
        } catch (SQLException e) {
            logger.error("Failed to link author to book", e);
            throw e;
        }
    }

    /**
     * Links a book with a genre (book_genre junction).
     *
     * @param bookId  the book ID
     * @param genreId the genre ID
     * @throws SQLException if database error occurs
     */
    public void addGenreToBook(int bookId, int genreId) throws SQLException {
        String sql = "INSERT INTO book_genre (book_id, genre_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            stmt.setInt(2, genreId);
            stmt.executeUpdate();
            logger.info("Genre {} linked to book {}", genreId, bookId);
        } catch (SQLException e) {
            logger.error("Failed to link genre to book", e);
            throw e;
        }
    }

    @Override
    public boolean update(Book book) throws SQLException {
        String sql = "UPDATE book SET isbn = ?, title = ?, publication_year = ?, " +
                "total_copies = ?, publisher_id = ?, description = ? WHERE book_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setInt(3, book.getPublicationYear());
            stmt.setInt(4, book.getTotalCopies());

            if (book.getPublisherId() != null) {
                stmt.setInt(5, book.getPublisherId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setString(6, book.getDescription());

            stmt.setInt(7, book.getBookId());

            int rows = stmt.executeUpdate();
            logger.info("Book updated: id={}", book.getBookId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update book: {}", book.getBookId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer bookId) throws SQLException {
        String sql = "DELETE FROM book WHERE book_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            int rows = stmt.executeUpdate();
            logger.info("Book deleted: id={}", bookId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete book: {}", bookId, e);
            throw e;
        }
    }

    /**
     * Returns total count of books for pagination.
     *
     * @return total book count
     * @throws SQLException if database error occurs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM book";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count books", e);
            throw e;
        }
        return 0;
    }

    /**
     * Searches books strictly by title (librarian inventory search).
     *
     * @param query  title fragment (case-insensitive)
     * @param limit  max records per page
     * @param offset records to skip
     * @return list of matching books
     * @throws SQLException if database error occurs
     */
    public List<Book> searchByTitle(String query, int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM book WHERE LOWER(title) LIKE LOWER(?) " +
                "ORDER BY title LIMIT ? OFFSET ?";
        List<Book> books = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + query + "%");
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to search books by title: {}", query, e);
            throw e;
        }
        return books;
    }

    /**
     * Counts books whose title matches the query (for inventory search pagination).
     */
    public int countByTitle(String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book WHERE LOWER(title) LIKE LOWER(?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + query + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count books by title: {}", query, e);
            throw e;
        }
        return 0;
    }

    /**
     * Counts books matching a search query (title or ISBN). For pagination of search results.
     */
    public int countSearch(String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book WHERE LOWER(title) LIKE LOWER(?) OR isbn LIKE ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count search results for query: {}", query, e);
            throw e;
        }
        return 0;
    }

    /**
     * Counts books in a given genre. For pagination of genre-filtered results.
     */
    public int countByGenre(int genreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book b " +
                "JOIN book_genre bg ON b.book_id = bg.book_id WHERE bg.genre_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, genreId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count books by genre: {}", genreId, e);
            throw e;
        }
        return 0;
    }

    /**
     * Counts available books (available_copies > 0). For pagination of the availability filter.
     */
    public int countAvailable() throws SQLException {
        String sql = "SELECT COUNT(*) FROM book WHERE available_copies > 0";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count available books", e);
            throw e;
        }
        return 0;
    }

    /**
     * Decrements available_copies by one (when a copy is borrowed/requested).
     * Guarded so the count never goes below zero.
     *
     * @param bookId the book ID
     * @return true if a row was updated
     * @throws SQLException if database error occurs
     */
    public boolean decrementAvailableCopies(int bookId) throws SQLException {
        String sql = "UPDATE book SET available_copies = available_copies - 1 " +
                "WHERE book_id = ? AND available_copies > 0";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to decrement available copies for book: {}", bookId, e);
            throw e;
        }
    }

    /**
     * Increments available_copies by one (when a copy is returned).
     * Guarded so the count never exceeds total_copies.
     *
     * @param bookId the book ID
     * @return true if a row was updated
     * @throws SQLException if database error occurs
     */
    public boolean incrementAvailableCopies(int bookId) throws SQLException {
        String sql = "UPDATE book SET available_copies = available_copies + 1 " +
                "WHERE book_id = ? AND available_copies < total_copies";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to increment available copies for book: {}", bookId, e);
            throw e;
        }
    }

    /**
     * Increases both total_copies and available_copies by the given amount
     * (used when a librarian adds new physical copies of a book).
     *
     * @param bookId the book ID
     * @param count  number of copies added
     * @return true if a row was updated
     * @throws SQLException if database error occurs
     */
    public boolean addCopies(int bookId, int count) throws SQLException {
        String sql = "UPDATE book SET total_copies = total_copies + ?, " +
                "available_copies = available_copies + ? WHERE book_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, count);
            stmt.setInt(2, count);
            stmt.setInt(3, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to add copies for book: {}", bookId, e);
            throw e;
        }
    }

    private Book mapResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setPublicationYear(rs.getInt("publication_year"));
        book.setTotalCopies(rs.getInt("total_copies"));
        book.setAvailableCopies(rs.getInt("available_copies"));

        int publisherId = rs.getInt("publisher_id");
        if (!rs.wasNull()) {
            book.setPublisherId(publisherId);
        }

        book.setDescription(rs.getString("description"));
        return book;
    }
}
