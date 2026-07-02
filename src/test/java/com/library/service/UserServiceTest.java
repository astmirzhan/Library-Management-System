package com.library.service;

import com.library.service.impl.UserServiceImpl;
import com.library.service.impl.BookServiceImpl;
import com.library.service.impl.BorrowServiceImpl;
import com.library.service.impl.ReviewServiceImpl;

import com.library.dao.UserDAO;
import com.library.model.User;
import com.library.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock UserDAO userDAO;
    private UserService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserServiceImpl(userDAO);
    }

    // ---------- authenticate ----------
    @Test
    void authenticateReturnsEmptyOnNullInput() throws Exception {
        assertTrue(service.authenticate(null, "x").isEmpty());
        assertTrue(service.authenticate("a@b.com", null).isEmpty());
    }

    @Test
    void authenticateReturnsEmptyWhenUserMissing() throws Exception {
        when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.empty());
        assertTrue(service.authenticate("a@b.com", "pw").isEmpty());
    }

    @Test
    void authenticateReturnsEmptyOnWrongPassword() throws Exception {
        User u = new User();
        u.setPasswordHash(PasswordUtil.hashPassword("correct"));
        when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        assertTrue(service.authenticate("a@b.com", "wrong").isEmpty());
    }

    @Test
    void authenticateSucceedsOnCorrectPassword() throws Exception {
        User u = new User();
        u.setEmail("a@b.com");
        u.setPasswordHash(PasswordUtil.hashPassword("secret"));
        when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        assertTrue(service.authenticate("a@b.com", "secret").isPresent());
    }

    // ---------- register (validation branches) ----------
    @Test
    void registerRejectsShortUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("ab", "a@b.com", "secret1", User.Role.READER, null));
    }

    @Test
    void registerRejectsBadEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("alice", "not-an-email", "secret1", User.Role.READER, null));
    }

    @Test
    void registerRejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("alice", "a@b.com", "123", User.Role.READER, null));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("alice", "a@b.com", "secret1", User.Role.READER, null));
    }

    @Test
    void registerSavesHashedUser() throws Exception {
        when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(userDAO.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register("alice", "a@b.com", "secret1", User.Role.READER, "123");
        assertNotEquals("secret1", saved.getPasswordHash());   // hashed
        assertEquals(User.Role.READER, saved.getRole());
    }

    // ---------- setUserActive ----------
    @Test
    void setUserActiveUpdatesFlag() throws Exception {
        User u = new User();
        u.setUserId(2);
        u.setActive(true);
        when(userDAO.findById(2)).thenReturn(Optional.of(u));
        when(userDAO.update(any())).thenReturn(true);

        assertTrue(service.setUserActive(2, false));
        assertFalse(u.isActive());
    }

    @Test
    void setUserActiveFalseWhenMissing() throws Exception {
        when(userDAO.findById(2)).thenReturn(Optional.empty());
        assertFalse(service.setUserActive(2, false));
    }

    // ---------- changePassword ----------
    @Test
    void changePasswordFailsOnWrongOldPassword() throws Exception {
        User u = new User();
        u.setPasswordHash(PasswordUtil.hashPassword("old"));
        when(userDAO.findById(1)).thenReturn(Optional.of(u));
        assertFalse(service.changePassword(1, "wrong", "newsecret"));
    }

    @Test
    void changePasswordSucceeds() throws Exception {
        User u = new User();
        u.setPasswordHash(PasswordUtil.hashPassword("old"));
        when(userDAO.findById(1)).thenReturn(Optional.of(u));
        when(userDAO.update(any())).thenReturn(true);
        assertTrue(service.changePassword(1, "old", "newsecret"));
    }

    @Test
    void deleteUserDelegates() throws Exception {
        when(userDAO.deleteById(5)).thenReturn(true);
        assertTrue(service.deleteUser(5));
    }
}
