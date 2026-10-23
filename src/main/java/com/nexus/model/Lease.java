package com.nexus.model;

import java.time.LocalDate;

/**
 * One row of the lease table, plus the property and tenant names (from a JOIN)
 * so the screens can display them. status: ACTIVE, EXPIRED, or TERMINATED.
 */
public record Lease(int leaseId, int propertyId, int tenantId,
                    LocalDate startDate, LocalDate endDate, double monthlyRent, String status,
                    String propertyName, String tenantName, String businessName) {
}
