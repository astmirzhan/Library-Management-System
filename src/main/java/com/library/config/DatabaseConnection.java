package com.library.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that owns a hand-written {@link ConnectionPool} and hands out pooled
 * connections. Configuration is read from application.properties. The pool is created
 * lazily on the first {@link #getConnection()} call so the object can be built without
 * a live database (e.g. in unit tests).
 *
 * <p>Implements the Singleton design pattern.
 */
public class DatabaseConnection {

    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);
    private static final int DEFAULT_POOL_SIZE = 10;

    private static DatabaseConnection instance;

    private final String url;
    private final String username;
    private final String password;
    private final String driver;
    private final int poolSize;

    private volatile ConnectionPool pool;

    private DatabaseConnection() {
        Properties props = loadProperties();
        this.url = props.getProperty("spring.datasource.url");
        this.username = props.getProperty("spring.datasource.username");
        this.password = props.getProperty("spring.datasource.password");
        this.driver = props.getProperty("spring.datasource.driver-class-name");
        this.poolSize = parseIntOrDefault(props.getProperty("db.pool.max.size"), DEFAULT_POOL_SIZE);
        loadDriver();
    }

    /**
     * Returns the single instance. Thread-safe lazy initialization.
     *
     * @return the DatabaseConnection instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Unable to find application.properties");
                throw new RuntimeException("application.properties not found");
            }
            props.load(input);
            logger.info("Database properties loaded successfully");
            return props;
        } catch (IOException e) {
            logger.error("Failed to load database properties", e);
            throw new RuntimeException("Failed to load database properties", e);
        }
    }

    private void loadDriver() {
        try {
            Class.forName(driver);
            logger.info("PostgreSQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC Driver not found", e);
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }

    /**
     * Borrows a connection from the custom pool (creating the pool on first use).
     * Return it by calling {@code close()} on the returned connection.
     *
     * @return a pooled Connection
     * @throws SQLException if the pool cannot be created / no connection is available
     */
    public Connection getConnection() throws SQLException {
        ConnectionPool p = pool;
        if (p == null) {
            synchronized (this) {
                if (pool == null) {
                    pool = ConnectionPool.create(url, username, password, poolSize);
                }
                p = pool;
            }
        }
        return p.takeConnection();
    }

    /**
     * Shuts the pool down (closes physical connections). Call on application stop.
     */
    public synchronized void shutdown() {
        if (pool != null) {
            pool.shutdown();
            pool = null;
        }
    }

    private int parseIntOrDefault(String value, int def) {
        if (value == null) return def;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
