package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The ONLY class that contains SQL for the activity_logs table (System Audit Log).
 * Every query uses a parameterized PreparedStatement.
 *
 * Columns: id, user_id, action_type, description, timestamp
 */
public class ActivityLogDao {

    private static final String INSERT =
            "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";

    private static final String SELECT =
            "SELECT a.id, a.user_id, u.username, a.action_type, a.description, a.timestamp "
                    + "FROM activity_logs a JOIN users u ON a.user_id = u.id ";

    // Newest first. id breaks ties for entries written in the same second.
    private static final String NEWEST_FIRST = "ORDER BY a.timestamp DESC, a.id DESC";

    /** Inserts one new log entry (opens its own connection). */
    public void logAction(int userId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            logAction(conn, userId, actionType, description);
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
        }
    }

    /**
     * Inserts one new log entry using a connection that is already open.
     * Other DAOs use this inside their own transaction, so the change and its
     * log entry are saved together (or rolled back together).
     */
    public void logAction(Connection conn, int userId, String actionType, String description)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }

    /** Returns every recorded action, newest first. */
    public List<ActivityLog> getAllLogs() {
        return query(SELECT + NEWEST_FIRST, null);
    }

    /** Returns the actions of one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT + "WHERE a.user_id = ? " + NEWEST_FIRST, userId);
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(rs.getInt("id"), rs.getInt("user_id"),
                            rs.getString("username"), rs.getString("action_type"),
                            rs.getString("description"), rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }
}
