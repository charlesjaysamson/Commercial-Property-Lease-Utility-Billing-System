package com.nexus.model;

/**
 * One row of the users table.
 * status is PENDING (new account), ACTIVE, or INACTIVE.
 */
public record User(int id, String username, String password, String roles, String status) {

    public boolean hasRole(Role role) {
        return roles.equals(role.name());
    }

    public boolean isActive() {
        return status.equals("ACTIVE");
    }
}
