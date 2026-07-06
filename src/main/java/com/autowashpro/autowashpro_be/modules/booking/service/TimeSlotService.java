package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAllSlots(boolean activeOnly) {
        List<TimeSlot> slots = activeOnly
                ? timeSlotRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                : timeSlotRepository.findAllByOrderByDisplayOrderAsc();
        return slots.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public TimeSlotResponse createSlot(TimeSlotRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        TimeSlot slot = TimeSlot.builder()
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxCapacity(request.getMaxCapacity())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .dayOfWeek(request.getDayOfWeek() != null ? request.getDayOfWeek() : "ALL")
                .build();

        slot = timeSlotRepository.save(slot);
        return mapToResponse(slot);
    }

    @Transactional
    public TimeSlotResponse updateSlot(Long id, TimeSlotRequest request) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setMaxCapacity(request.getMaxCapacity());
        if (request.getIsActive() != null) {
            slot.setIsActive(request.getIsActive());
        }
        if (request.getDisplayOrder() != null) {
            slot.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getDayOfWeek() != null) {
            slot.setDayOfWeek(request.getDayOfWeek());
        }

        return mapToResponse(timeSlotRepository.save(slot));
    }

    @Transactional
    public TimeSlotResponse toggleStatus(Long id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));
        slot.setIsActive(!slot.getIsActive());
        return mapToResponse(timeSlotRepository.save(slot));
    }

    public TimeSlotResponse mapToResponse(TimeSlot entity) {
        return TimeSlotResponse.builder()
                .slotId(entity.getSlotId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .maxCapacity(entity.getMaxCapacity())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .dayOfWeek(entity.getDayOfWeek())
                .build();
    }
}
