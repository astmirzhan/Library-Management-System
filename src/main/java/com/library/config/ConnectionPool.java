package com.library.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

/**
 * A hand-written, thread-safe JDBC connection pool.
 *
 * <p>It keeps a fixed number of real {@link Connection}s in two
 * {@link BlockingQueue}s: one for free connections and one for the ones currently
 * in use. Callers borrow a connection with {@link #takeConnection()} (which blocks
 * if the pool is empty) and return it simply by calling {@code close()} — thanks to
 * the {@link PooledConnection} wrapper (Decorator pattern), {@code close()} puts the
 * connection back into the pool instead of really closing it.
 */
public class ConnectionPool {

    private static final Logger logger = LogManager.getLogger(ConnectionPool.class);

    private final BlockingQueue<Connection> availableConnections;
    private final BlockingQueue<Connection> usedConnections;
    private final int poolSize;

    private ConnectionPool(int poolSize) {
        this.poolSize = poolSize;
        this.availableConnections = new ArrayBlockingQueue<>(poolSize);
        this.usedConnections = new ArrayBlockingQueue<>(poolSize);
    }

    /**
     * Factory method: opens {@code poolSize} real connections and wraps each of them
     * in a {@link PooledConnection}.
     *
     * @param url      JDBC URL
     * @param user     database user
     * @param password database password
     * @param poolSize number of physical connections to open
     * @return an initialised pool
     * @throws SQLException if a connection cannot be opened
     */
    public static ConnectionPool create(String url, String user, String password, int poolSize)
            throws SQLException {
        ConnectionPool pool = new ConnectionPool(poolSize);
        for (int i = 0; i < poolSize; i++) {
            Connection real = DriverManager.getConnection(url, user, password);
            pool.availableConnections.offer(new PooledConnection(real, pool));
        }
        logger.info("Custom ConnectionPool initialised with {} connections", poolSize);
        return pool;
    }

    /**
     * Borrows a connection, blocking until one is free.
     *
     * @return a pooled connection (return it by calling {@code close()})
     */
    public Connection takeConnection() {
        try {
            Connection connection = availableConnections.take();   // blocks if empty
            usedConnections.offer(connection);
            logger.debug("Connection taken (free={}, used={})",
                    availableConnections.size(), usedConnections.size());
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for a free connection", e);
        }
    }

    /**
     * Returns a connection to the pool (called by {@link PooledConnection#close()}).
     */
    void releaseConnection(Connection connection) {
        usedConnections.remove(connection);
        availableConnections.offer(connection);
        logger.debug("Connection released (free={}, used={})",
                availableConnections.size(), usedConnections.size());
    }

    /** Number of connections currently available. */
    public int availableCount() {
        return availableConnections.size();
    }

    /** Total pool size. */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Really closes every physical connection (call on application shutdown).
     */
    public void shutdown() {
        for (Connection c : availableConnections) {
            closeReal(c);
        }
        for (Connection c : usedConnections) {
            closeReal(c);
        }
        availableConnections.clear();
        usedConnections.clear();
        logger.info("ConnectionPool shut down");
    }

    private void closeReal(Connection c) {
        try {
            if (c instanceof PooledConnection) {
                ((PooledConnection) c).reallyClose();
            } else {
                c.close();
            }
        } catch (SQLException e) {
            logger.warn("Failed to close a pooled connection", e);
        }
    }

    /**
     * Wrapper around a real {@link Connection} (Decorator pattern). Every method just
     * delegates to the wrapped connection, except {@link #close()}, which returns the
     * connection to the pool instead of closing it.
     */
    static final class PooledConnection implements Connection {

        private final Connection connection;
        private final ConnectionPool pool;

        PooledConnection(Connection connection, ConnectionPool pool) {
            this.connection = connection;
            this.pool = pool;
        }

        /** Returns this connection to the pool instead of closing it. */
        @Override
        public void close() throws SQLException {
            if (connection.getAutoCommit() == false) {
                connection.setAutoCommit(true);
            }
            pool.releaseConnection(this);
        }

        /** Actually closes the underlying physical connection (used on shutdown). */
        void reallyClose() throws SQLException {
            connection.close();
        }

        // ---- everything below simply delegates to the wrapped connection ----

        @Override public Statement createStatement() throws SQLException { return connection.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return connection.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return connection.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return connection.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { connection.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return connection.getAutoCommit(); }
        @Override public void commit() throws SQLException { connection.commit(); }
        @Override public void rollback() throws SQLException { connection.rollback(); }
        @Override public boolean isClosed() throws SQLException { return connection.isClosed(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return connection.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { connection.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return connection.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { connection.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return connection.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { connection.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return connection.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return connection.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { connection.clearWarnings(); }
        @Override public Statement createStatement(int rs, int rc) throws SQLException { return connection.createStatement(rs, rc); }
        @Override public PreparedStatement prepareStatement(String sql, int rs, int rc) throws SQLException { return connection.prepareStatement(sql, rs, rc); }
        @Override public CallableStatement prepareCall(String sql, int rs, int rc) throws SQLException { return connection.prepareCall(sql, rs, rc); }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return connection.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { connection.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { connection.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return connection.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return connection.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return connection.setSavepoint(name); }
        @Override public void rollback(Savepoint sp) throws SQLException { connection.rollback(sp); }
        @Override public void releaseSavepoint(Savepoint sp) throws SQLException { connection.releaseSavepoint(sp); }
        @Override public Statement createStatement(int rs, int rc, int rh) throws SQLException { return connection.createStatement(rs, rc, rh); }
        @Override public PreparedStatement prepareStatement(String sql, int rs, int rc, int rh) throws SQLException { return connection.prepareStatement(sql, rs, rc, rh); }
        @Override public CallableStatement prepareCall(String sql, int rs, int rc, int rh) throws SQLException { return connection.prepareCall(sql, rs, rc, rh); }
        @Override public PreparedStatement prepareStatement(String sql, int keys) throws SQLException { return connection.prepareStatement(sql, keys); }
        @Override public PreparedStatement prepareStatement(String sql, int[] idx) throws SQLException { return connection.prepareStatement(sql, idx); }
        @Override public PreparedStatement prepareStatement(String sql, String[] names) throws SQLException { return connection.prepareStatement(sql, names); }
        @Override public Clob createClob() throws SQLException { return connection.createClob(); }
        @Override public Blob createBlob() throws SQLException { return connection.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return connection.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return connection.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return connection.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { connection.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties props) throws SQLClientInfoException { connection.setClientInfo(props); }
        @Override public String getClientInfo(String name) throws SQLException { return connection.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return connection.getClientInfo(); }
        @Override public Array createArrayOf(String type, Object[] elements) throws SQLException { return connection.createArrayOf(type, elements); }
        @Override public Struct createStruct(String type, Object[] attrs) throws SQLException { return connection.createStruct(type, attrs); }
        @Override public void setSchema(String schema) throws SQLException { connection.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return connection.getSchema(); }
        @Override public void abort(Executor executor) throws SQLException { connection.abort(executor); }
        @Override public void setNetworkTimeout(Executor executor, int ms) throws SQLException { connection.setNetworkTimeout(executor, ms); }
        @Override public int getNetworkTimeout() throws SQLException { return connection.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return connection.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return connection.isWrapperFor(iface); }
    }
}
