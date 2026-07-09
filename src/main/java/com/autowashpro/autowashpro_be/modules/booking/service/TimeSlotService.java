package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final RealtimeNotificationService realtimeNotificationService;

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

        validateNoOverlap(request.getStartTime(), request.getEndTime(), request.getDayOfWeek(), null);

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

        validateNoOverlap(request.getStartTime(), request.getEndTime(), request.getDayOfWeek(), id);

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

        boolean targetStatus = !slot.getIsActive();
        if (!targetStatus) {
            // Admin đang muốn ĐÓNG (tắt) khung giờ này -> Kiểm tra ràng buộc dịch vụ & thông báo Khách hàng
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // 1. Kiểm tra ràng buộc: Nếu đang trong giờ phục vụ hiện tại và có đơn IN_PROGRESS
            if (now.isAfter(slot.getStartTime()) && now.isBefore(slot.getEndTime())) {
                int inProgressCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(today, id, List.of(BookingStatus.IN_PROGRESS));
                if (inProgressCount > 0) {
                    throw new BadRequestException("⚠️ Ràng buộc dịch vụ: Không thể đóng khung giờ đang trong thời gian phục vụ có đơn hàng đang rửa (IN_PROGRESS)!");
                }
            }

            // 2. Tự động truy vấn đơn đặt lịch sắp tới và gửi thông báo khẩn tới Khách hàng bị ảnh hưởng
            List<Booking> affectedBookings = bookingRepository.findAllByTimeSlotSlotIdAndBookingDateGreaterThanEqualAndStatusIn(
                    id, today, Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            for (Booking booking : affectedBookings) {
                String title = "⚠️ Thông báo khẩn: Thay đổi lịch bảo trì bãi rửa";
                String content = String.format("Khung giờ %s - %s ngày %s đã tạm thời bị đóng do bảo trì khẩn cấp/sự cố kỹ thuật. Vui lòng liên hệ Hotline trạm hoặc đặt lại giờ khác. NovaWash chân thành xin lỗi quý khách!",
                        slot.getStartTime(), slot.getEndTime(), booking.getBookingDate());

                realtimeNotificationService.notifyBookingStatusChanged(booking, NotificationType.SYSTEM_ALERT, title, content);
                log.info("Đã gửi thông báo khẩn cấp đóng khung giờ tới khách hàng {} (Đơn: {})", booking.getCustomer().getPhoneNumber(), booking.getBookingCode());
            }
        }

        slot.setIsActive(targetStatus);
        return mapToResponse(timeSlotRepository.save(slot));
    }

    private void validateNoOverlap(LocalTime start, LocalTime end, String dayOfWeek, Long excludeId) {
        List<TimeSlot> existingSlots = timeSlotRepository.findAll();
        for (TimeSlot existing : existingSlots) {
            if (excludeId != null && existing.getSlotId().equals(excludeId)) {
                continue;
            }
            if (!Boolean.TRUE.equals(existing.getIsActive())) {
                continue;
            }

            String d1 = dayOfWeek != null ? dayOfWeek : "ALL";
            String d2 = existing.getDayOfWeek() != null ? existing.getDayOfWeek() : "ALL";
            boolean dayConflict = d1.equalsIgnoreCase("ALL") || d2.equalsIgnoreCase("ALL") || d1.equalsIgnoreCase(d2);

            if (dayConflict) {
                LocalTime s1 = start;
                LocalTime e1 = end;
                LocalTime s2 = existing.getStartTime();
                LocalTime e2 = existing.getEndTime();

                if (s1.isBefore(e2) && s2.isBefore(e1)) {
                    throw new BadRequestException("⚠️ Lỗi trùng lặp: Khung giờ [" + s1 + " - " + e1 + 
                            "] bị giao thoa thời gian với khung giờ hoạt động sẵn có [" + s2 + " - " + e2 + "] của cấu hình ngày " + d2 + "!");
                }
            }
        }
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
