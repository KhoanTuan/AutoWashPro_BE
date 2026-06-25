package com.autowashpro.autowashpro_be.modules.capacity.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.modules.booking.entity.Slot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.SlotRepository;
import com.autowashpro.autowashpro_be.modules.capacity.dto.SlotAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotAvailabilityService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> getAvailability(LocalDate date) {
        final LocalDate targetDate = (date == null) ? LocalDate.now() : date;

        if (targetDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot view availability for a past date");
        }

        return slotRepository.findAll(Sort.by("startTime").ascending()).stream()
                .map(slot -> toAvailability(slot, targetDate)) //
                .toList();
    }

    @Transactional(readOnly = true)
    public void ensureSlotAvailable(Integer slotId, LocalDate bookingDate) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));

        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book a slot in the past");
        }

        long booked = bookingRepository.countBySlotSlotIdAndBookingDate(slotId, bookingDate);
        if (booked >= slot.getMaxCapacity()) {
            throw new BadRequestException("Selected slot is fully booked on " + bookingDate);
        }
    }

    private SlotAvailabilityResponse toAvailability(Slot slot, LocalDate date) {
        long booked = bookingRepository.countBySlotSlotIdAndBookingDate(slot.getSlotId(), date);
        int available = Math.max(0, slot.getMaxCapacity() - (int) booked);

        return SlotAvailabilityResponse.builder()
                .slotId(slot.getSlotId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .maxCapacity(slot.getMaxCapacity())
                .bookedCount(booked)
                .availableSpots(available)
                .open(available > 0)
                .build();
    }
}
