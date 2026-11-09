package com.scc.dao;

import com.scc.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * All SQL for the users table. Every query uses PreparedStatement.
 */
public class UserDao {

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Normal registration. The status is always PENDING. Returns the new id, or -1 on failure. */
    public int register(String username, String password, String role) {
        return insert(username, password, role, "PENDING");
    }

    /** Used only on first run, when no accounts exist yet. This account starts ACTIVE. */
    public int registerFirstAdministrator(String username, String password) {
        return insert(username, password, "ADMINISTRATOR", "ACTIVE");
    }

    private int insert(String username, String password, String role, String status) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);          // saved exactly as typed
            ps.setString(3, role);
            ps.setString(4, status);
            if (ps.executeUpdate() == 1) {
                // Ask SQLite for the id it just generated (same connection, so it is ours).
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Checks the username and password against the database.
     * Passwords are stored exactly as typed, so they are readable in the users table.
     * Accounts made by an older version of this program stored a SHA-256 hash instead;
     * those still log in, and their password is converted to plain text on the first login.
     */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ?";
        User found = null;
        boolean oldHashedAccount = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    if (stored.equals(password)) {
                        found = map(rs);
                    } else if (stored.equals(legacyHash(password))) {
                        found = map(rs);
                        oldHashedAccount = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (found == null) {
            return Optional.empty();
        }
        if (oldHashedAccount) {
            // Done after the read is finished, so SQLite is not locked by our own query.
            savePlainPassword(found.id(), password);
            found = new User(found.id(), found.username(), password, found.roles(), found.status());
        }
        return Optional.of(found);
    }

    private void savePlainPassword(int userId, String password) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, password);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public List<User> findAll() {
        return query("SELECT id, username, password, roles, status FROM users ORDER BY id", null);
    }

    public List<User> findByStatus(String status) {
        return query("SELECT id, username, password, roles, status FROM users "
                + "WHERE status = ? ORDER BY id", status);
    }

    private final ActivityLogDao logDao = new ActivityLogDao();

    /**
     * Approves a PENDING account (status becomes ACTIVE) and writes the audit log
     * entry in the same transaction.
     * Log example: "Administrator [1] approved User [2] (samson)"
     */
    public boolean approveUser(User admin, User target) {
        String sql = "UPDATE users SET status = 'ACTIVE' WHERE id = ? AND status = 'PENDING'";
        String description = admin.actor() + " approved User [" + target.id() + "] (" + target.username() + ")";
        return updateAndLog(sql, ps -> ps.setInt(1, target.id()),
                admin.id(), "USER_APPROVED", description);
    }

    /**
     * Changes only the status of an account (deactivate / reactivate) and logs it.
     * Log example: "Administrator [1] changed status of User [2] (samson) to INACTIVE"
     */
    public boolean updateStatus(User admin, User target, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        String description = admin.actor() + " changed status of User [" + target.id() + "] ("
                + target.username() + ") to " + status;
        return updateAndLog(sql, ps -> {
            ps.setString(1, status);
            ps.setInt(2, target.id());
        }, admin.id(), "USER_UPDATED", description);
    }

    /**
     * Changes the role and status of one user (used by the Administrator) and logs it.
     * Both values are converted to UPPER CASE before they go to the database,
     * so "active" and "ACTIVE" are saved the same way.
     * Log example: "Administrator [1] updated User [3] (brent): Role changed to
     * PROPERTY_MANAGER, Status changed to ACTIVE"
     */
    public boolean updateUserRoleAndStatus(User admin, User target, String newRole, String newStatus) {
        String role = newRole.toUpperCase(Locale.ROOT);
        String status = newStatus.toUpperCase(Locale.ROOT);
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";
        String description = admin.actor() + " updated User [" + target.id() + "] (" + target.username()
                + "): Role changed to " + role + ", Status changed to " + status;
        return updateAndLog(sql, ps -> {
            ps.setString(1, role);
            ps.setString(2, status);
            ps.setInt(3, target.id());
        }, admin.id(), "USER_UPDATED", description);
    }

    /** Sets the parameters of one PreparedStatement. */
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    /**
     * Runs ONE update and, if exactly one row changed, writes the audit log entry
     * on the same connection. If the log cannot be written, the update is rolled back.
     */
    private boolean updateAndLog(String sql, ParamSetter setter,
                                 int actorId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setter.set(ps);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
                logDao.logAction(conn, actorId, actionType, description);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private List<User> query(String sql, String param) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return users;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                rs.getString("roles"), rs.getString("status"));
    }

    // Only used to recognise accounts created by the older, hashed version.
    private String legacyHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
