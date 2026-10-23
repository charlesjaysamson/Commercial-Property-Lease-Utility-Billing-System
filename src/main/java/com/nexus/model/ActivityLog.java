package com.nexus.model;

/**
 * One row of the activity_logs table (with the username from a JOIN).
 */
public record ActivityLog(int logId, int userId, String username,
                          String action, String description, String logTimestamp) {
}
