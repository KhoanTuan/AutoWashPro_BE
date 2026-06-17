package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
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

    @Schema(description = "true = nhân viên phải đổi mật khẩu ở lần login tiếp theo", example = "false")
    private Boolean requirePasswordChange;

    @Schema(description = "Trạng thái tài khoản", example = "ACTIVE")
    private StaffStatus status;

    @Schema(description = "Các vai trò đang được gán")
    private List<RoleSummary> roles;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private LocalDateTime updatedAt;
}
