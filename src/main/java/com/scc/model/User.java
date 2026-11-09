package com.scc.model;

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

    /** Short label used in the audit log, for example "Administrator [1]". */
    public String actor() {
        StringBuilder label = new StringBuilder();
        for (String word : roles.toLowerCase().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label + " [" + id + "]";
    }
}
