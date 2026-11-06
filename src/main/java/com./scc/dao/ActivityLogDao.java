package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes and reads the activity_logs table (System Audit Log).
 * Every query uses a parameterized PreparedStatement.
 */
public class ActivityLogDao {

    private static final String SELECT =
            "SELECT a.log_id, a.user_id, u.username, a.action, a.description, a.log_timestamp "
                    + "FROM activity_logs a JOIN users u ON a.user_id = u.id ";

    // Newest first. log_id breaks ties for entries written in the same second.
    private static final String NEWEST_FIRST = "ORDER BY a.log_timestamp DESC, a.log_id DESC";

    /** Inserts one new log entry. */
    public void logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
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
                    logs.add(new ActivityLog(rs.getInt("log_id"), rs.getInt("user_id"),
                            rs.getString("username"), rs.getString("action"),
                            rs.getString("description"), rs.getString("log_timestamp")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }
}
