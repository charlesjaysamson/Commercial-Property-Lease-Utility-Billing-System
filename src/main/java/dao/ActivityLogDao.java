package dao;

import model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes and reads the activity_logs table (System Activity Log).
 * Events recorded: CREATE, UPDATE, DELETE.
 */
public class ActivityLogDao {

    public void log(int userId, String action, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
        }
    }

    public List<ActivityLog> findRecent(int limit) {
        String sql = "SELECT a.log_id, a.user_id, u.username, a.action, a.description, a.log_timestamp "
                + "FROM activity_logs a JOIN users u ON a.user_id = u.id "
                + "ORDER BY a.log_id DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
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
