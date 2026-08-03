package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu đặt lịch rửa xe từ Khách hàng")
public class CreateBookingRequest {

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(min = 5, max = 20, message = "Biển số xe phải từ 5 đến 20 ký tự")
    @Schema(description = "Biển số xe máy (tối thiểu 5 ký tự, không được trùng với tài khoản khác)", example = "29-D1 555.55")
    private String licensePlate;

    @Size(max = 50, message = "Dòng xe tối đa 50 ký tự")
    @Schema(description = "Dòng xe / tên xe (ví dụ: Yamaha Grande, Honda SH, Vespa...)", example = "Yamaha Grande")
    private String model;

    @NotNull(message = "Ngày đặt lịch không được để trống")
    @Schema(description = "Ngày tới xưởng rửa xe (YYYY-MM-DD, không được rơi vào ngày nghỉ lễ hoặc quá khứ)", example = "2026-07-07")
    private LocalDate bookingDate;

    @NotNull(message = "ID khung giờ không được để trống")
    @Schema(description = "ID khung giờ (phải còn trống suất phục vụ và áp dụng đúng thứ trong tuần)", example = "1")
    private Long timeSlotId;

    @NotNull(message = "ID gói dịch vụ không được để trống")
    @Schema(description = "ID gói dịch vụ chính (ví dụ: 1 = Rửa tiêu chuẩn, 2 = Rửa cao cấp... phải đang kinh doanh)", example = "1")
    private Long packageId;

    @Schema(description = "Danh sách ID các dịch vụ chọn thêm (nếu có, ví dụ: tẩy xích, vệ sinh mũ bảo hiểm...)", example = "[4, 5]")
    private List<Long> addonIds;

    @Schema(description = "Ghi chú đặc biệt cho kỹ thuật viên xưởng", example = "Rửa cẩn thận gầm xe và xịt khô xích")
    private String notes;

    @Schema(description = "Mã voucher/khuyến mãi áp dụng từ ví của khách", example = "VOU-WELCOME50-8819")
    private String voucherCode;
}
