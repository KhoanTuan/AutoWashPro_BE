package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hạng thành viên VIP & quyền lợi đặc quyền")
public class LoyaltyTierResponse {

    @Schema(description = "ID hạng VIP", example = "1")
    private Integer tierId;

    @Schema(description = "Mã hạng hệ thống", example = "REGULAR")
    private String tierName;

    @Schema(description = "Tên hiển thị trên App Khách hàng", example = "Member")
    private String displayName;

    @Schema(description = "Mức chi tiêu tối thiểu để đạt hạng (VND)", example = "0")
    private BigDecimal minSpend;

    @Schema(description = "Hệ số tích điểm thưởng", example = "1.00")
    private BigDecimal tierMultiplier;

    @Schema(description = "Số ngày tối đa được đặt lịch trước (booking window)", example = "7")
    private Integer bookingWindowDays;

    @Schema(description = "Tóm tắt quyền lợi cho giao diện", example = "Đặt trước 7 ngày • Tích điểm x1.00")
    private String benefitsSummary;

    // Các trường dành riêng khi kiểm tra trạng thái cá nhân (/my-benefits)
    @Schema(description = "Tổng chi tiêu hiện tại của khách hàng (VND)", example = "650000.00")
    private BigDecimal currentSpend;

    @Schema(description = "Tên hạng VIP kế tiếp", example = "Silver")
    private String nextTierDisplayName;

    @Schema(description = "Mức chi tiêu cần đạt cho hạng kế tiếp (VND)", example = "1000000.00")
    private BigDecimal nextTierMinSpend;

    @Schema(description = "Số tiền cần chi tiêu thêm để lên hạng (VND)", example = "350000.00")
    private BigDecimal spendNeededForNextTier;

    @Schema(description = "Phần trăm tiến độ lên hạng kế tiếp (0 - 100%)", example = "65")
    private Integer progressPercentage;
}
