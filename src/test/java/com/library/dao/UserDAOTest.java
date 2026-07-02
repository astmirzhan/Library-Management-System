package com.library.dao;

import com.library.dao.impl.*;

import com.library.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserDAOTest {

    private JdbcTestSupport.Mocks m;
    private UserDAO dao;

    @BeforeEach
    void setUp() {
        m = JdbcTestSupport.newMocks();
        dao = new UserDAOImpl();
        JdbcTestSupport.inject(dao, m.dbConnection);
    }

    private void stubUserRow() throws Exception {
        when(m.resultSet.getInt("user_id")).thenReturn(3);
        when(m.resultSet.getString("user_uuid")).thenReturn("uuid-3");
        when(m.resultSet.getString("username")).thenReturn("alice");
        when(m.resultSet.getString("email")).thenReturn("alice@mail.com");
        when(m.resultSet.getString("password")).thenReturn("$2a$hash");
        when(m.resultSet.getString("role")).thenReturn("READER");
        when(m.resultSet.getDate("registration_date")).thenReturn(Date.valueOf(LocalDate.now()));
        when(m.resultSet.getString("phone_number")).thenReturn("123");
        when(m.resultSet.getBoolean("active")).thenReturn(true);
    }

    @Test
    void findByIdReturnsMappedUser() throws Exception {
        when(m.resultSet.next()).thenReturn(true);
        stubUserRow();

        Optional<User> result = dao.findById(3);

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        assertEquals(User.Role.READER, result.get().getRole());
        assertTrue(result.get().isActive());
    }

    @Test
    void findByIdReturnsEmptyWhenNoRow() throws Exception {
        when(m.resultSet.next()).thenReturn(false);
        assertTrue(dao.findById(99).isEmpty());
    }

    @Test
    void findByEmailReturnsUser() throws Exception {
        when(m.resultSet.next()).thenReturn(true);
        stubUserRow();
        Optional<User> result = dao.findByEmail("alice@mail.com");
        assertTrue(result.isPresent());
        assertEquals("alice@mail.com", result.get().getEmail());
    }

    @Test
    void saveReturnsUserWithGeneratedId() throws Exception {
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt("user_id")).thenReturn(42);

        User user = new User();
        user.setUsername("bob");
        user.setEmail("bob@mail.com");
        user.setPasswordHash("h");
        user.setRole(User.Role.READER);

        User saved = dao.save(user);
        assertEquals(42, saved.getUserId());
        assertNotNull(saved.getUserUuid());          // generated
        assertNotNull(saved.getRegistrationDate());  // generated
    }

    @Test
    void updateReturnsTrueOnRowChange() throws Exception {
        User user = new User();
        user.setUserId(3);
        user.setUsername("alice2");
        user.setEmail("a@mail.com");
        user.setPasswordHash("h");
        user.setRole(User.Role.READER);
        assertTrue(dao.update(user));
    }

    @Test
    void deleteByIdReturnsTrue() throws Exception {
        assertTrue(dao.deleteById(3));
    }

    @Test
    void countReturnsValue() throws Exception {
        when(m.resultSet.next()).thenReturn(true);
        when(m.resultSet.getInt(1)).thenReturn(7);
        assertEquals(7, dao.count());
    }

    @Test
    void findAllPaginatedMapsRows() throws Exception {
        when(m.resultSet.next()).thenReturn(true, false);
        stubUserRow();
        List<User> users = dao.findAll(10, 0);
        assertEquals(1, users.size());
    }

    @Test
    void findByRoleMapsRows() throws Exception {
        when(m.resultSet.next()).thenReturn(true, false);
        stubUserRow();
        List<User> users = dao.findByRole(User.Role.READER);
        assertEquals(1, users.size());
    }
}
