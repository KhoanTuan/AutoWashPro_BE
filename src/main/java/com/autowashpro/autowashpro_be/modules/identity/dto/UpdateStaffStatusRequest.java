package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request đổi trạng thái nhân viên")
public class UpdateStaffStatusRequest {

    @NotNull
    @Schema(description = "ACTIVE = hoạt động, INACTIVE = bị khóa (không đặt PENDING_ACTIVATION thủ công)", example = "INACTIVE")
    private StaffStatus status;
}
