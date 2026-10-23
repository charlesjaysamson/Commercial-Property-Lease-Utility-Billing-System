package com.nexus.model;

import java.time.LocalDate;

/**
 * One row of the billing table, plus names from a JOIN for display.
 * billingPeriod is stored as YYYY-MM. paymentStatus: UNPAID, PAID, or OVERDUE.
 */
public record Billing(int billingId, int leaseId, String billingPeriod,
                      double rentAmount, double utilityAmount, double lateFee,
                      double totalAmount, double amountPaid,
                      LocalDate billingDate, LocalDate dueDate, String paymentStatus,
                      String propertyName, String tenantName, String businessName) {
}
