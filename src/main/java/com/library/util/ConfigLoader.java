package com.library.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton utility class for loading application configuration.
 * Reads values from application.properties.
 */
public class ConfigLoader {

    private static final Logger logger = LogManager.getLogger(ConfigLoader.class);
    private static ConfigLoader instance;
    private final Properties properties;

    private ConfigLoader() {
        properties = new Properties();
        loadProperties();
    }

    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                logger.error("application.properties not found");
                throw new RuntimeException("application.properties not found");
            }

            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    public int getMaxBorrowingLimit() {
        return getInt("library.max.borrowing.limit", 5);
    }

    public int getDefaultBorrowPeriodDays() {
        return getInt("library.default.borrow.period.days", 14);
    }

    public double getDailyFineAmount() {
        String value = properties.getProperty("library.daily.fine.amount", "1.0");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }
}