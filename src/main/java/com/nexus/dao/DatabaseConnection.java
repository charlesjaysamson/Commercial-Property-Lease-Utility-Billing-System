package com.nexus.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the SQLite file.
 * Foreign key rules are switched on for EVERY connection (SQLite has them OFF by default).
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:lease_billing.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }
}
