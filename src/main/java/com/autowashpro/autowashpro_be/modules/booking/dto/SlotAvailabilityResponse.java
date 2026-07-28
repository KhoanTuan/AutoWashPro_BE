package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin trạng thái khả dụng của 1 khung giờ trong ngày")
public class SlotAvailabilityResponse {
    private Long slotId;

    @Schema(type = "string", example = "08:00")
    private LocalTime startTime;

    @Schema(type = "string", example = "09:00")
    private LocalTime endTime;

    private Integer maxCapacity;
    private Integer bookedCount;
    private Integer availableCapacity;
    private Boolean isAvailable;
    private String disabledReason; // null if available, otherwise "FULL", "MAINTENANCE", "PAST", or "CLOSED_HOLIDAY: reason"

    @Schema(description = "true nếu ngày này bị khóa toàn bộ (garage_closure)", example = "false")
    @Builder.Default
    private Boolean isDayLocked = false;

    @Schema(description = "Lý do đóng cửa/khóa ngày (nếu isDayLocked = true)", example = "Bảo trì thiết bị")
    private String closureReason;
}

