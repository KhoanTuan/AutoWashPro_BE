package com.autowashpro.autowashpro_be.modules.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to claim a vehicle from the shop-floor queue")
public class ClaimTaskRequest {

    @NotNull
    @Schema(description = "Technician staff ID claiming the bay task", example = "3")
    private Long technicianId;
}
