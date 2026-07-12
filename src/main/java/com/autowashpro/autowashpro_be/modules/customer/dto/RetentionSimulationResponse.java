package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả mô phỏng nhiệm vụ retention Loyalty")
public class RetentionSimulationResponse {

    @Schema(description = "Trạng thái trả về", example = "success")
    private String status;

    @Schema(description = "Tổng điểm loyalty đã bị hết hạn", example = "1200")
    private Integer expiredPoints;

    @Schema(description = "Số lượng khách hàng VIP bị hạ hạng", example = "8")
    private Integer downgradedUsers;

    @Schema(description = "Số lượng khách hàng bị chuyển sang INACTIVE", example = "5")
    private Integer deactivatedUsers;
}
