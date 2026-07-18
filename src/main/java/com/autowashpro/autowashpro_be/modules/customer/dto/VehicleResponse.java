package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin xe của khách hàng trong garage profile")
public class VehicleResponse {

    @Schema(description = "ID xe", example = "1")
    private Long vehicleId;

    @Schema(description = "ID chủ xe", example = "10")
    private Long customerId;

    @Schema(description = "Biển số xe", example = "59-P1 234.56")
    private String licensePlate;

    @Schema(description = "Dòng xe / model", example = "Honda SH Mode 2024")
    private String model;

    @Schema(description = "Thời gian thêm xe vào hệ thống")
    private LocalDateTime createdAt;

    @Schema(description = "Có phải xe mặc định không", example = "true")
    private Boolean isDefault;
}
