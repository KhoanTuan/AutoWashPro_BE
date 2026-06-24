package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Cập nhật thông tin nhân viên — không bao gồm KPI/rating/jobs")
public class UpdateStaffRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Họ tên", example = "Nguyen Van B")
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Email", example = "staff@autowashpro.com")
    private String email;

    @Size(max = 15)
    @Schema(description = "Số điện thoại nội bộ", example = "0912345678")
    private String phoneNumber;

    @Schema(description = "Danh sách roleId — ghi đè roles hiện tại nếu gửi")
    private List<Integer> roleIds;
}
