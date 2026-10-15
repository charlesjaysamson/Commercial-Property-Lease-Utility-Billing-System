package dao;

import model.Property;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the property table.
 */
public class PropertyDao {

    private static final String SELECT =
            "SELECT property_id, property_name, location, rental_rate, status FROM property ";

    public boolean existsByName(String propertyName) {
        String sql = "SELECT 1 FROM property WHERE LOWER(property_name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, propertyName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** New properties always start AVAILABLE. */
    public boolean add(String propertyName, String location, double rentalRate) {
        String sql = "INSERT INTO property (property_name, location, rental_rate, status) "
                + "VALUES (?, ?, ?, 'AVAILABLE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, propertyName);
            ps.setString(2, location);
            ps.setDouble(3, rentalRate);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public List<Property> findAll() {
        return query(SELECT + "ORDER BY property_name", null);
    }

    public List<Property> findByStatus(String status) {
        return query(SELECT + "WHERE status = ? ORDER BY property_name", status);
    }

    /** Searches the property name and the location. */
    public List<Property> search(String keyword) {
        String like = "%" + keyword + "%";
        List<Property> list = new ArrayList<>();
        String sql = SELECT + "WHERE property_name LIKE ? OR location LIKE ? ORDER BY property_name";
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

    public boolean updateStatus(int propertyId, String status) {
        String sql = "UPDATE property SET status = ? WHERE property_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, propertyId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM property WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }

    private List<Property> query(String sql, String param) {
        List<Property> list = new ArrayList<>();
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

    private Property map(ResultSet rs) throws SQLException {
        return new Property(rs.getInt("property_id"), rs.getString("property_name"),
                rs.getString("location"), rs.getDouble("rental_rate"), rs.getString("status"));
    }
}
