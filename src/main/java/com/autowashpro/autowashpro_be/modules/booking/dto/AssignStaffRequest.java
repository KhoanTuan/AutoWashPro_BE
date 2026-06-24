package com.autowashpro.autowashpro_be.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignStaffRequest {
    @NotNull
    private Long technicianId;
}
