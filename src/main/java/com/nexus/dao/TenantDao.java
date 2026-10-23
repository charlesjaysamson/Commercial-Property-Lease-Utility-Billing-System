package com.nexus.dao;

import com.nexus.model.Tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the tenant table.
 */
public class TenantDao {

    private static final String SELECT =
            "SELECT tenant_id, tenant_name, business_name, contact_number, email, status FROM tenant ";

    /** New tenants always start ACTIVE. */
    public boolean add(String tenantName, String businessName, String contactNumber, String email) {
        String sql = "INSERT INTO tenant (tenant_name, business_name, contact_number, email, status) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantName);
            ps.setString(2, businessName);
            ps.setString(3, contactNumber);
            ps.setString(4, email);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public List<Tenant> findAll() {
        return query(SELECT + "ORDER BY tenant_name", null);
    }

    public List<Tenant> findByStatus(String status) {
        return query(SELECT + "WHERE status = ? ORDER BY tenant_name", status);
    }

    /** Searches the tenant name and the business name. */
    public List<Tenant> search(String keyword) {
        String like = "%" + keyword + "%";
        List<Tenant> list = new ArrayList<>();
        String sql = SELECT + "WHERE tenant_name LIKE ? OR business_name LIKE ? ORDER BY tenant_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateInfo(int tenantId, String tenantName, String businessName,
                              String contactNumber, String email) {
        String sql = "UPDATE tenant SET tenant_name = ?, business_name = ?, contact_number = ?, "
                + "email = ? WHERE tenant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantName);
            ps.setString(2, businessName);
            ps.setString(3, contactNumber);
            ps.setString(4, email);
            ps.setInt(5, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int tenantId, String status) {
        String sql = "UPDATE tenant SET status = ? WHERE tenant_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, tenantId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private List<Tenant> query(String sql, String param) {
        List<Tenant> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    private Tenant map(ResultSet rs) throws SQLException {
        return new Tenant(rs.getInt("tenant_id"), rs.getString("tenant_name"),
                rs.getString("business_name"), rs.getString("contact_number"),
                rs.getString("email"), rs.getString("status"));
    }
}
