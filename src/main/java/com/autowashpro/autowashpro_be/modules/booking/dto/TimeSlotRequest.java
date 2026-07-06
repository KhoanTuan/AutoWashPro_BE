package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotRequest {

    @NotNull(message = "Start time must not be null")
    @Schema(type = "string", example = "08:00", description = "Giờ bắt đầu (HH:mm)")
    private LocalTime startTime;

    @NotNull(message = "End time must not be null")
    @Schema(type = "string", example = "09:00", description = "Giờ kết thúc (HH:mm)")
    private LocalTime endTime;

    @NotNull(message = "Max capacity must not be null")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Schema(example = "5", description = "Công suất tối đa cho khung giờ này")
    private Integer maxCapacity;

    @Builder.Default
    @Schema(example = "true", description = "Trạng thái hoạt động")
    private Boolean isActive = true;

    @Builder.Default
    @Schema(example = "0", description = "Thứ tự hiển thị trên UI")
    private Integer displayOrder = 0;

    @Builder.Default
    @Schema(example = "ALL", description = "Áp dụng theo thứ: ALL, WEEKDAY, WEEKEND, MON, TUE, WED, THU, FRI, SAT, SUN")
    private String dayOfWeek = "ALL";
}

