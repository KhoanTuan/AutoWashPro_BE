package com.autowashpro.autowashpro_be.modules.operations.service;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingType;
import com.autowashpro.autowashpro_be.modules.booking.entity.Slot;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueLane;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Double-queue routing engine combining appointment schedule, loyalty tier, and check-in punctuality.
 */
@Component
public class QueueRoutingEngine {

    private static final double TIER_WEIGHT = 0.35;
    private static final double SLOT_WEIGHT = 0.40;
    private static final double CHECK_IN_WEIGHT = 0.25;

    public QueueLane resolveLane(Booking booking) {
        return booking.getBookingType() == BookingType.APP_BOOKING
                ? QueueLane.APPOINTMENT
                : QueueLane.WALK_IN;
    }

    public double computePriorityScore(Booking booking, LocalDateTime checkInTime) {
        double tierScore = resolveTierScore(booking.getCustomer().getTier());
        double checkInScore = resolveCheckInScore(checkInTime);

        if (booking.getBookingType() == BookingType.APP_BOOKING) {
            double slotScore = resolveSlotScore(booking.getBookingDate(), booking.getSlot(), checkInTime);
            return round(tierScore * TIER_WEIGHT + slotScore * SLOT_WEIGHT + checkInScore * CHECK_IN_WEIGHT);
        }

        // Walk-in lane: tier loyalty and FIFO check-in dominate.
        return round(tierScore * 0.60 + checkInScore * 0.40);
    }

    private double resolveTierScore(LoyaltyTier tier) {
        if (tier == null || tier.getTierName() == null) {
            return 250.0;
        }
        return switch (tier.getTierName().toUpperCase()) {
            case "PLATINUM", "VIP" -> 1000.0;
            case "GOLD" -> 750.0;
            case "SILVER" -> 500.0;
            default -> 250.0;
        };
    }

    /**
     * Higher score when the customer is closer to (or just past) the scheduled slot start.
     */
    private double resolveSlotScore(LocalDate bookingDate, Slot slot, LocalDateTime checkInTime) {
        LocalDateTime slotStart = LocalDateTime.of(bookingDate, slot.getStartTime());
        long minutesUntilSlot = Duration.between(checkInTime, slotStart).toMinutes();

        if (minutesUntilSlot <= 0) {
            // On-time or late: urgency increases as lateness grows (capped).
            return 1000.0 + Math.min(Math.abs(minutesUntilSlot), 120);
        }
        // Early arrival: still prioritized, but less than on-time customers.
        return Math.max(0, 800.0 - minutesUntilSlot);
    }

    /**
     * Earlier check-ins receive a higher score within the same business day.
     */
    private double resolveCheckInScore(LocalDateTime checkInTime) {
        LocalTime time = checkInTime.toLocalTime();
        int minutesFromOpen = time.getHour() * 60 + time.getMinute() - (8 * 60);
        return Math.max(0, 600.0 - minutesFromOpen);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
