package com.scc.model;

/**
 * One row of the activity_logs table (with the username from a JOIN).
 */
public record ActivityLog(int id, int userId, String username,
                          String actionType, String description, String timestamp) {
}
