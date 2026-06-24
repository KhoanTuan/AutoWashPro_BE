package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Thông tin nhân viên (không bao gồm password)")
public class StaffResponse {

    @Schema(description = "ID nhân viên", example = "1")
    private Long staffId;

    @Schema(description = "Username", example = "cashier")
    private String username;

    @Schema(description = "Email", example = "cashier@autowashpro.com")
    private String email;

    @Schema(description = "Họ tên", example = "Demo Cashier")
    private String fullName;

    @Schema(description = "Số điện thoại nội bộ", example = "0912345678")
    private String phoneNumber;

    @Schema(description = "true = nhân viên phải đổi mật khẩu ở lần login tiếp theo", example = "false")
    private Boolean requirePasswordChange;

    @Schema(description = "Trạng thái tài khoản PENDING_ACTIVATION | ACTIVE | INACTIVE", example = "ACTIVE")
    private StaffStatus status;

    @Schema(description = "Trạng thái ca: IDLE | BUSY | ON_BREAK | OFF", example = "IDLE")
    private StaffWorkStatus workStatus;

    @Schema(description = "Nhãn hiển thị UI: On-duty | In-break | Off-duty", example = "On-duty")
    private String workStatusLabel;

    @Schema(description = "Tên vai trò hiển thị (job title)", example = "Lead Technician")
    private String roleLabel;

    @Schema(description = "Tổng số job đã hoàn thành", example = "142")
    private Integer totalJobs;

    @Schema(description = "Hiệu suất KPI 0–100 (read-only)", example = "98.0")
    private Double efficiency;

    @Schema(description = "Điểm rating 0–5 (read-only)", example = "4.9")
    private Double rating;

    @Schema(description = "Các vai trò đang được gán")
    private List<RoleSummary> roles;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private LocalDateTime updatedAt;

    @Schema(description = "Thời điểm soft delete — null nếu chưa xóa")
    private LocalDateTime deletedAt;
}
