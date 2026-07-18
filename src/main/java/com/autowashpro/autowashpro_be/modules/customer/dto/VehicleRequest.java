package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm/sửa thông tin xe (garage cá nhân)")
public class VehicleRequest {

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 20, message = "Biển số xe tối đa 20 ký tự")
    @Schema(description = "Biển số xe", example = "59-P1 234.56")
    private String licensePlate;

    @Size(max = 100, message = "Tên dòng xe tối đa 100 ký tự")
    @Schema(description = "Dòng xe / model", example = "Honda SH Mode 2024")
    private String model;

    @Schema(description = "Có phải xe mặc định không", example = "true")
    private Boolean isDefault;
}
