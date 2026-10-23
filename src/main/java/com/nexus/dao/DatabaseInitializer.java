package com.nexus.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes sure every table exists BEFORE the menu starts.
 * "CREATE TABLE IF NOT EXISTS" means it is safe to run on every start:
 * existing tables and their data are never touched.
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String[] tables = {
                """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL,
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS property (
                    property_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    property_name TEXT NOT NULL UNIQUE,
                    location      TEXT NOT NULL,
                    rental_rate   REAL NOT NULL CHECK (rental_rate >= 0),
                    status        TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS tenant (
                    tenant_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    tenant_name    TEXT NOT NULL,
                    business_name  TEXT NOT NULL,
                    contact_number TEXT NOT NULL,
                    email          TEXT NOT NULL,
                    status         TEXT NOT NULL DEFAULT 'ACTIVE'
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS lease (
                    lease_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    property_id  INTEGER NOT NULL,
                    tenant_id    INTEGER NOT NULL,
                    start_date   TEXT NOT NULL,
                    end_date     TEXT NOT NULL,
                    monthly_rent REAL NOT NULL CHECK (monthly_rent >= 0),
                    status       TEXT NOT NULL DEFAULT 'ACTIVE',
                    FOREIGN KEY (property_id) REFERENCES property (property_id),
                    FOREIGN KEY (tenant_id)   REFERENCES tenant (tenant_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS billing (
                    billing_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    lease_id       INTEGER NOT NULL,
                    billing_period TEXT NOT NULL,
                    rent_amount    REAL NOT NULL,
                    utility_amount REAL NOT NULL,
                    late_fee       REAL NOT NULL DEFAULT 0,
                    total_amount   REAL NOT NULL,
                    amount_paid    REAL NOT NULL DEFAULT 0,
                    billing_date   TEXT NOT NULL,
                    due_date       TEXT NOT NULL,
                    payment_status TEXT NOT NULL DEFAULT 'UNPAID',
                    FOREIGN KEY (lease_id) REFERENCES lease (lease_id),
                    UNIQUE (lease_id, billing_period)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id       INTEGER NOT NULL,
                    action        TEXT NOT NULL,
                    description   TEXT NOT NULL,
                    log_timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (user_id) REFERENCES users (id)
                )
                """
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database setup failed: " + e.getMessage(), e);
        }
    }
}
