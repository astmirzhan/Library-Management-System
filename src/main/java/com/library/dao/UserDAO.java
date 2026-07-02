package com.library.dao;

import com.library.model.User;

import java.sql.SQLException;
import java.util.List;

/** DAO contract for the USER table. */
public interface UserDAO extends BaseDAO<User, Integer> {
    java.util.Optional<User> findByEmail(String email) throws SQLException;
    List<User> findAll(int limit, int offset) throws SQLException;
    List<User> findByRole(User.Role role) throws SQLException;
    int count() throws SQLException;
}
