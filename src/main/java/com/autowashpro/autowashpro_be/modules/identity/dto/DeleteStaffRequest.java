package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request xóa nhân viên — mặc định soft delete")
public class DeleteStaffRequest {

    @Schema(description = "true = xóa vĩnh viễn khỏi DB; false = soft delete (mặc định)", example = "false")
    private Boolean hardDelete = false;
}
