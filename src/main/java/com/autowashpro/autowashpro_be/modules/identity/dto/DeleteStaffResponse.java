package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Kết quả xóa nhân viên")
public class DeleteStaffResponse {

    @Schema(description = "staffId đã xóa", example = "13")
    private Long staffId;

    @Schema(description = "SOFT hoặc HARD", example = "SOFT")
    private String deletionType;

    @Schema(description = "Thông báo", example = "Staff soft deleted successfully")
    private String message;
}
