package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Cập nhật trạng thái ca làm việc (On-duty / In-break / Off-duty)")
public class UpdateStaffWorkStatusRequest {

    @NotNull
    @Schema(description = "IDLE | BUSY | ON_BREAK | OFF", example = "IDLE")
    private StaffWorkStatus workStatus;
}
