package model;

/**
 * One row of the tenant table. status: ACTIVE or INACTIVE.
 */
public record Tenant(int tenantId, String tenantName, String businessName,
                     String contactNumber, String email, String status) {
}
