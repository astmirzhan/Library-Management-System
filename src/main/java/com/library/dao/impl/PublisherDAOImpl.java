package com.library.dao.impl;

import com.library.dao.*;

import com.library.config.DatabaseConnection;
import com.library.model.Publisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for the PUBLISHER table.
 * Provides CRUD operations for publishers.
 */
@Repository
public class PublisherDAOImpl implements PublisherDAO {

    private static final Logger logger = LogManager.getLogger(PublisherDAOImpl.class);
    private DatabaseConnection dbConnection;

    public PublisherDAOImpl() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public Publisher save(Publisher publisher) throws SQLException {
        String sql = "INSERT INTO publisher (publisher_uuid, name, address, website) " +
                "VALUES (?, ?, ?, ?) RETURNING publisher_id";

        if (publisher.getPublisherUuid() == null) {
            publisher.setPublisherUuid(UUID.randomUUID().toString());
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, publisher.getPublisherUuid());
            stmt.setString(2, publisher.getName());
            stmt.setString(3, publisher.getAddress());
            stmt.setString(4, publisher.getWebsite());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    publisher.setPublisherId(rs.getInt("publisher_id"));
                    logger.info("Publisher saved: id={}, name={}",
                            publisher.getPublisherId(), publisher.getName());
                    return publisher;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save publisher: {}", publisher.getName(), e);
            throw e;
        }
        throw new SQLException("Failed to save publisher");
    }

    @Override
    public Optional<Publisher> findById(Integer publisherId) throws SQLException {
        String sql = "SELECT * FROM publisher WHERE publisher_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, publisherId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find publisher by id: {}", publisherId, e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Publisher> findAll() throws SQLException {
        String sql = "SELECT * FROM publisher ORDER BY name";
        List<Publisher> publishers = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                publishers.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all publishers", e);
            throw e;
        }
        return publishers;
    }

    @Override
    public boolean update(Publisher publisher) throws SQLException {
        String sql = "UPDATE publisher SET name = ?, address = ?, website = ? WHERE publisher_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, publisher.getName());
            stmt.setString(2, publisher.getAddress());
            stmt.setString(3, publisher.getWebsite());
            stmt.setInt(4, publisher.getPublisherId());

            int rows = stmt.executeUpdate();
            logger.info("Publisher updated: id={}", publisher.getPublisherId());
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to update publisher: {}", publisher.getPublisherId(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteById(Integer publisherId) throws SQLException {
        String sql = "DELETE FROM publisher WHERE publisher_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, publisherId);
            int rows = stmt.executeUpdate();
            logger.info("Publisher deleted: id={}", publisherId);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete publisher: {}", publisherId, e);
            throw e;
        }
    }

    private Publisher mapResultSet(ResultSet rs) throws SQLException {
        Publisher publisher = new Publisher();
        publisher.setPublisherId(rs.getInt("publisher_id"));
        publisher.setPublisherUuid(rs.getString("publisher_uuid"));
        publisher.setName(rs.getString("name"));
        publisher.setAddress(rs.getString("address"));
        publisher.setWebsite(rs.getString("website"));
        return publisher;
    }
}
