package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

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
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp   TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
                )
                """
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            upgradeActivityLogsTable(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Database setup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Upgrades an OLD activity_logs table (columns log_id / action / log_timestamp,
     * or a foreign key without ON DELETE CASCADE) to the current layout:
     * id, user_id, action_type, description, timestamp + ON DELETE CASCADE.
     * Every existing row is copied over, so no log history is lost.
     * If the table is already up to date, nothing happens.
     */
    private static void upgradeActivityLogsTable(Connection conn) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(activity_logs)")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }

        boolean cascade = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_list(activity_logs)")) {
            while (rs.next()) {
                if ("CASCADE".equalsIgnoreCase(rs.getString("on_delete"))) {
                    cascade = true;
                }
            }
        }

        boolean currentColumns = columns.contains("id") && columns.contains("action_type")
                && columns.contains("timestamp");
        if (currentColumns && cascade) {
            return; // already up to date
        }

        // Work out the names the old table used for each value.
        String oldId = columns.contains("id") ? "id" : "log_id";
        String oldAction = columns.contains("action_type") ? "action_type" : "action";
        String oldTime = columns.contains("timestamp") ? "timestamp" : "log_timestamp";

        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE activity_logs RENAME TO activity_logs_old");
            stmt.execute("""
                    CREATE TABLE activity_logs (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id     INTEGER NOT NULL,
                        action_type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp   TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("INSERT INTO activity_logs (id, user_id, action_type, description, timestamp) "
                    + "SELECT " + oldId + ", user_id, " + oldAction + ", description, " + oldTime
                    + " FROM activity_logs_old WHERE user_id IN (SELECT id FROM users) ORDER BY "
                    + oldId);
            stmt.execute("DROP TABLE activity_logs_old");
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }
}
