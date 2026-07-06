package com.autowashpro.autowashpro_be.modules.notification.dto;

import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Thông tin thông báo trong quả chuông 🔔")
public class NotificationResponse {

    @Schema(description = "ID thông báo", example = "1")
    private Long notificationId;

    @Schema(description = "Tiêu đề thông báo", example = "🎉 Đặt lịch thành công!")
    private String title;

    @Schema(description = "Nội dung chi tiết", example = "Mã NV-8891 cho khung giờ 08:00 - 09:00 ngày 05/07 đã được tiếp nhận.")
    private String content;

    @Schema(description = "Loại thông báo", example = "NEW_BOOKING")
    private NotificationType type;

    @Schema(description = "Mã tham chiếu (Mã đơn booking NV-...)", example = "NV-260705-1234")
    private String referenceCode;

    @Schema(description = "Trạng thái đã đọc hay chưa", example = "false")
    private Boolean isRead;

    @Schema(description = "Thời gian tạo thông báo", example = "05/07/2026 11:15")
    private String createdAtFormatted;
}
