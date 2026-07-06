package com.autowashpro.autowashpro_be.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Số lượng thông báo chưa đọc (badge icon chuông)")
public class NotificationStatsResponse {

    @Schema(description = "Số thông báo chưa xem", example = "3")
    private long unreadCount;
}
