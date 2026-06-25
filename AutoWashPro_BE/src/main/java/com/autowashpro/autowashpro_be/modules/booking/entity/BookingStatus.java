package com.autowashpro.autowashpro_be.modules.booking.entity;

/**
 * Strict booking lifecycle states (Phase 1 state machine):
 * CREATE → PAY → ASSIGN → ACCEPT → START → COMPLETE
 *
 * PENDING → PAID → ASSIGNED → ACCEPTED → PROCESSING → COMPLETED
 */
public enum BookingStatus {
    PENDING,
    PAID,
    ASSIGNED,
    ACCEPTED,
    PROCESSING,
    COMPLETED
}
