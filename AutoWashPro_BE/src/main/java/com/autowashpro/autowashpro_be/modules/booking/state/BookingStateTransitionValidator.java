package com.autowashpro.autowashpro_be.modules.booking.state;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;

/**
 * Enforces the sequential booking lifecycle state machine.
 */
public final class BookingStateTransitionValidator {

    private BookingStateTransitionValidator() {
    }

    /**
     * Validates a single forward transition in the booking lifecycle.
     *
     * @param currentStatus the booking's current status
     * @param nextStatus    the requested next status
     * @throws BadRequestException when the transition is invalid or the lifecycle is terminal
     */
    public static void validateStateTransition(BookingStatus currentStatus, BookingStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            throw new BadRequestException("Booking status cannot be null");
        }

        if (currentStatus == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot modify booking status: lifecycle is already COMPLETED");
        }

        if (currentStatus == nextStatus) {
            throw new BadRequestException("Booking is already in status " + nextStatus);
        }

        BookingStatus expectedNext = expectedNextStatus(currentStatus);
        if (expectedNext != nextStatus) {
            throw new BadRequestException(
                    "Invalid state transition: " + currentStatus + " -> " + nextStatus
                            + ". Expected next state: " + expectedNext
            );
        }
    }

    private static BookingStatus expectedNextStatus(BookingStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> BookingStatus.PAID;
            case PAID -> BookingStatus.ASSIGNED;
            case ASSIGNED -> BookingStatus.ACCEPTED;
            case ACCEPTED -> BookingStatus.PROCESSING;
            case PROCESSING -> BookingStatus.COMPLETED;
            case COMPLETED -> throw new BadRequestException(
                    "Cannot modify booking status: lifecycle is already COMPLETED"
            );
        };
    }
}
