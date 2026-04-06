package com.student.analyzer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - Singleton JDBC connection manager.
 * Supports environment variables for cloud deployment (Aiven, Render, etc.)
 * Fallback to local XAMPP if no env vars are defined.
 */
public class DBConnection {

    // ===== DATABASE CONFIGURATION (with environment variable support) =====
    private static final String DEFAULT_URL  = "jdbc:postgresql://ep-late-night-a8qs310v-pooler.eastus2.azure.neon.tech/neondb?sslmode=require&channel_binding=require";
    private static final String DEFAULT_USER = "neondb_owner";
    private static final String DEFAULT_PASS = "npg_RUQEry7Dub9h";

    // Static connection instance (singleton pattern)
    private static Connection connection = null;

    // Private constructor — cannot instantiate this class
    private DBConnection() {}

    /**
     * Returns a live database Connection.
     */
    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                // 1. Load PostgreSQL JDBC driver
                Class.forName("org.postgresql.Driver");

                // 2. Get credentials from Environment Variables (for Cloud) or use defaults (for Local)
                String envUrl  = System.getenv("DB_URL");
                String envUser = System.getenv("DB_USER");
                String envPass = System.getenv("DB_PASS");

                String url  = (envUrl  != null) ? envUrl  : DEFAULT_URL;
                String user = (envUser != null) ? envUser : DEFAULT_USER;
                String pass = (envPass != null) ? envPass : DEFAULT_PASS;

                // 3. Connect
                System.out.println("[DBConnection] Attempting to connect to: " + maskUrl(url));
                connection = DriverManager.getConnection(url, user, pass);
                System.out.println("[DBConnection] Successfully connected to database.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] JDBC Driver not found: " + e.getMessage());
            throw new SQLException("JDBC Driver not found", e);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection failed: " + e.getMessage());
            throw e;
        }
        return connection;
    }

    /**
     * Masks the URL but shows the hostname for debugging.
     */
    private static String maskUrl(String url) {
        if (url == null) return "null";
        try {
            // Extract the part after // and before the first / or ?
            String host = url.split("//")[1].split("/|\\?")[0];
            return "host=" + host;
        } catch (Exception e) {
            return "Local or unknown URL format";
        }
    }

    /**
     * Closes the current connection.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DBConnection] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
        }
    }
}
