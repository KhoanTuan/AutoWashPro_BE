package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStaffStatusRequest {
    @NotNull
    private StaffStatus status;
}
