package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hồ sơ khách hàng — dùng cho customer dashboard")
public class CustomerProfileResponse {

    @Schema(description = "ID khách hàng", example = "1")
    private Long customerId;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Họ tên", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "Hạng thành viên hệ thống", example = "REGULAR")
    private String tierName;

    @Schema(description = "Tên hạng hiển thị trên App (Member, Silver, Gold, Platinum)", example = "Member")
    private String tierDisplayName;

    @Schema(description = "Số ngày tối đa được đặt trước theo hạng VIP", example = "7")
    private Integer bookingWindowDays;

    @Schema(description = "Số lần ghé trạm", example = "5")
    private Integer visitCount;

    @Schema(description = "Tổng chi tiêu (VND)", example = "1500000.00")
    private BigDecimal totalSpending;

    @Schema(description = "Chi tiêu trong chu kỳ xét hạng hiện tại (VND)", example = "500000.00")
    private BigDecimal tierSpending;

    @Schema(description = "Điểm tích lũy hiện tại", example = "150")
    private Integer loyaltyPoints;

    @Schema(description = "Danh sách xe trong Garage cá nhân")
    private List<VehicleResponse> vehicles;
}
