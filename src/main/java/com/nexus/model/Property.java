package com.nexus.model;

/**
 * One row of the property table. status: AVAILABLE, OCCUPIED, or INACTIVE.
 */
public record Property(int propertyId, String propertyName, String location,
                       double rentalRate, String status) {
}
