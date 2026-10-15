package dao;

import model.Lease;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the lease table. Creating or ending a lease also changes the
 * property status, so those two changes are done in ONE transaction: either
 * both succeed or neither does.
 */
public class LeaseDao {

    private static final String SELECT =
            "SELECT l.lease_id, l.property_id, l.tenant_id, l.start_date, l.end_date, "
                    + "l.monthly_rent, l.status, p.property_name, t.tenant_name, t.business_name "
                    + "FROM lease l "
                    + "JOIN property p ON l.property_id = p.property_id "
                    + "JOIN tenant t ON l.tenant_id = t.tenant_id ";

    public boolean createLease(int propertyId, int tenantId, LocalDate startDate,
                               LocalDate endDate, double monthlyRent) {
        String occupySql = "UPDATE property SET status = 'OCCUPIED' "
                + "WHERE property_id = ? AND status = 'AVAILABLE'";
        String insertSql = "INSERT INTO lease (property_id, tenant_id, start_date, end_date, "
                + "monthly_rent, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement occupy = conn.prepareStatement(occupySql);
                 PreparedStatement insert = conn.prepareStatement(insertSql)) {

                occupy.setInt(1, propertyId);
                if (occupy.executeUpdate() != 1) {
                    conn.rollback(); // property was not AVAILABLE
                    return false;
                }

                insert.setInt(1, propertyId);
                insert.setInt(2, tenantId);
                insert.setString(3, startDate.toString());
                insert.setString(4, endDate.toString());
                insert.setDouble(5, monthlyRent);
                insert.executeUpdate();

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

    public List<Lease> findAll() {
        return query(SELECT + "ORDER BY l.lease_id DESC", null);
    }

    public List<Lease> findActive() {
        return query(SELECT + "WHERE l.status = ? ORDER BY t.business_name", "ACTIVE");
    }

    public boolean tenantHasActiveLease(int tenantId) {
        String sql = "SELECT 1 FROM lease WHERE tenant_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Changes the end date and/or monthly rent of an ACTIVE lease. */
    public boolean updateTerms(int leaseId, LocalDate endDate, double monthlyRent) {
        String sql = "UPDATE lease SET end_date = ?, monthly_rent = ? "
                + "WHERE lease_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endDate.toString());
            ps.setDouble(2, monthlyRent);
            ps.setInt(3, leaseId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft-delete: sets the lease status to EXPIRED or TERMINATED (the row stays
     * in the table) and makes the property AVAILABLE again, in one transaction.
     */
    public boolean endLease(int leaseId, int propertyId, String newStatus) {
        String endSql = "UPDATE lease SET status = ? WHERE lease_id = ? AND status = 'ACTIVE'";
        String freeSql = "UPDATE property SET status = 'AVAILABLE' WHERE property_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement end = conn.prepareStatement(endSql);
                 PreparedStatement free = conn.prepareStatement(freeSql)) {

                end.setString(1, newStatus);
                end.setInt(2, leaseId);
                if (end.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }

                free.setInt(1, propertyId);
                free.executeUpdate();

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

    private List<Lease> query(String sql, String param) {
        List<Lease> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Lease(rs.getInt("lease_id"), rs.getInt("property_id"),
                            rs.getInt("tenant_id"), LocalDate.parse(rs.getString("start_date")),
                            LocalDate.parse(rs.getString("end_date")), rs.getDouble("monthly_rent"),
                            rs.getString("status"), rs.getString("property_name"),
                            rs.getString("tenant_name"), rs.getString("business_name")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }
}
