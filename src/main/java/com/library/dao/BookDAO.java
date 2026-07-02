package com.library.dao;

import com.library.model.Book;

import java.sql.SQLException;
import java.util.List;

/** DAO contract for the BOOK table. */
public interface BookDAO extends BaseDAO<Book, Integer> {
    List<Book> findAll(int limit, int offset) throws SQLException;
    List<Book> search(String query, int limit, int offset) throws SQLException;
    List<Book> findByGenre(int genreId, int limit, int offset) throws SQLException;
    List<Book> findByAuthor(int authorId) throws SQLException;
    List<Book> findAvailable(int limit, int offset) throws SQLException;
    void addAuthorToBook(int bookId, int authorId) throws SQLException;
    void addGenreToBook(int bookId, int genreId) throws SQLException;
    int count() throws SQLException;
    List<Book> searchByTitle(String query, int limit, int offset) throws SQLException;
    int countByTitle(String query) throws SQLException;
    int countSearch(String query) throws SQLException;
    int countByGenre(int genreId) throws SQLException;
    int countAvailable() throws SQLException;
    boolean decrementAvailableCopies(int bookId) throws SQLException;
    boolean incrementAvailableCopies(int bookId) throws SQLException;
    boolean addCopies(int bookId, int count) throws SQLException;
}
