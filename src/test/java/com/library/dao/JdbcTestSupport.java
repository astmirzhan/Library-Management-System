package com.library.dao;

import com.library.config.DatabaseConnection;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared helpers for DAO unit tests: builds a mocked JDBC chain and injects a mocked
 * DatabaseConnection into a DAO's private {@code dbConnection} field via reflection.
 */
final class JdbcTestSupport {

    private JdbcTestSupport() {
    }

    /** Holder for the mocked JDBC objects so a test can stub the ResultSet. */
    static final class Mocks {
        final DatabaseConnection dbConnection = mock(DatabaseConnection.class);
        final Connection connection = mock(Connection.class);
        final PreparedStatement preparedStatement = mock(PreparedStatement.class);
        final Statement statement = mock(Statement.class);
        final ResultSet resultSet = mock(ResultSet.class);
    }

    /**
     * Wires connection → prepareStatement/createStatement → executeQuery/executeUpdate → resultSet.
     */
    static Mocks newMocks() {
        Mocks m = new Mocks();
        try {
            when(m.dbConnection.getConnection()).thenReturn(m.connection);
            when(m.connection.prepareStatement(anyString())).thenReturn(m.preparedStatement);
            when(m.connection.createStatement()).thenReturn(m.statement);
            when(m.preparedStatement.executeQuery()).thenReturn(m.resultSet);
            when(m.statement.executeQuery(anyString())).thenReturn(m.resultSet);
            when(m.preparedStatement.executeUpdate()).thenReturn(1);
            when(m.statement.executeUpdate(anyString())).thenReturn(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    /**
     * Replaces the {@code dbConnection} field of a DAO instance with the mock.
     */
    static void inject(Object dao, DatabaseConnection mockDbConnection) {
        try {
            Field field = dao.getClass().getDeclaredField("dbConnection");
            field.setAccessible(true);
            field.set(dao, mockDbConnection);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock DatabaseConnection", e);
        }
    }
}
