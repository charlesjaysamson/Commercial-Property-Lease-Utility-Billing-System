package com.nexus.dao;

import com.nexus.model.Billing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the billing table.
 */
public class BillingDao {

    /** Late fee = 5% of (rent + utilities), added once when a bill becomes OVERDUE. */
    public static final double LATE_FEE_RATE = 0.05;

    private static final String SELECT =
            "SELECT b.billing_id, b.lease_id, b.billing_period, b.rent_amount, b.utility_amount, "
                    + "b.late_fee, b.total_amount, b.amount_paid, b.billing_date, b.due_date, "
                    + "b.payment_status, p.property_name, t.tenant_name, t.business_name "
                    + "FROM billing b "
                    + "JOIN lease l ON b.lease_id = l.lease_id "
                    + "JOIN property p ON l.property_id = p.property_id "
                    + "JOIN tenant t ON l.tenant_id = t.tenant_id ";

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public boolean existsForLeaseAndPeriod(int leaseId, String billingPeriod) {
        String sql = "SELECT 1 FROM billing WHERE lease_id = ? AND billing_period = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            ps.setString(2, billingPeriod);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** New bills start UNPAID with no late fee. */
    public boolean create(int leaseId, String billingPeriod, double rentAmount, double utilityAmount,
                          LocalDate billingDate, LocalDate dueDate) {
        String sql = "INSERT INTO billing (lease_id, billing_period, rent_amount, utility_amount, "
                + "late_fee, total_amount, amount_paid, billing_date, due_date, payment_status) "
                + "VALUES (?, ?, ?, ?, 0, ?, 0, ?, ?, 'UNPAID')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            ps.setString(2, billingPeriod);
            ps.setDouble(3, rentAmount);
            ps.setDouble(4, utilityAmount);
            ps.setDouble(5, round2(rentAmount + utilityAmount));
            ps.setString(6, billingDate.toString());
            ps.setString(7, dueDate.toString());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Any UNPAID bill whose due date has passed becomes OVERDUE and gets the late fee.
     * Because only UNPAID bills are touched, the fee is never added twice.
     */
    public int markOverdueBills() {
        String sql = "UPDATE billing SET payment_status = 'OVERDUE', "
                + "late_fee = ROUND((rent_amount + utility_amount) * ?, 2), "
                + "total_amount = rent_amount + utility_amount "
                + "+ ROUND((rent_amount + utility_amount) * ?, 2) "
                + "WHERE payment_status = 'UNPAID' AND due_date < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, LATE_FEE_RATE);
            ps.setDouble(2, LATE_FEE_RATE);
            ps.setString(3, LocalDate.now().toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }

    public boolean markPaid(int billingId, double amountPaid) {
        String sql = "UPDATE billing SET payment_status = 'PAID', amount_paid = ? "
                + "WHERE billing_id = ? AND payment_status <> 'PAID'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amountPaid);
            ps.setInt(2, billingId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public List<Billing> findAll() {
        return query(SELECT + "ORDER BY b.billing_period DESC, b.billing_id DESC", null);
    }

    /** Bills that still need payment (UNPAID or OVERDUE), earliest due date first. */
    public List<Billing> findUnsettled() {
        return query(SELECT + "WHERE b.payment_status IN ('UNPAID', 'OVERDUE') "
                + "ORDER BY b.due_date, b.billing_id", null);
    }

    public List<Billing> findByPeriod(String billingPeriod) {
        return query(SELECT + "WHERE b.billing_period = ? ORDER BY t.business_name", billingPeriod);
    }

    public int count() {
        return countWhere("SELECT COUNT(*) FROM billing", null);
    }

    public int countByStatus(String paymentStatus) {
        return countWhere("SELECT COUNT(*) FROM billing WHERE payment_status = ?", paymentStatus);
    }

    private int countWhere(String sql, String param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }

    private List<Billing> query(String sql, String param) {
        List<Billing> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Billing(rs.getInt("billing_id"), rs.getInt("lease_id"),
                            rs.getString("billing_period"), rs.getDouble("rent_amount"),
                            rs.getDouble("utility_amount"), rs.getDouble("late_fee"),
                            rs.getDouble("total_amount"), rs.getDouble("amount_paid"),
                            LocalDate.parse(rs.getString("billing_date")),
                            LocalDate.parse(rs.getString("due_date")),
                            rs.getString("payment_status"), rs.getString("property_name"),
                            rs.getString("tenant_name"), rs.getString("business_name")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }
}
