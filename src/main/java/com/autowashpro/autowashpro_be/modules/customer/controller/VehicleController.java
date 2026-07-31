package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.customer.service.VehicleService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vehicle Management", description = "Quản lý Phương tiện — Xóa xe với ràng buộc kiểm tra lịch đặt (Booking Guard Validation)")
public class VehicleController {

    private final VehicleService vehicleService;

    @DeleteMapping("/{vehicleId}")
    @Operation(
            summary = "[DELETE] Xóa phương tiện theo Vehicle ID (`DELETE /api/v1/vehicles/{vehicleId}`)",
            description = "Xóa phương tiện theo ID. Tự động kiểm tra ràng buộc tất cả các đơn đặt hàng liên quan:\n" +
                          "- **Chờ xử lý (PENDING):** Từ chối xóa nếu có đơn đặt hàng PENDING.\n" +
                          "- **Chưa hoàn thành tương lai (CONFIRMED / IN_PROGRESS / COMPLETED tương lai):** Từ chối xóa nếu scheduled booking time > CURRENT_TIMESTAMP.\n" +
                          "- **Đã hoàn thành trong quá khứ / Đã hủy:** Cho phép xóa (Soft-delete reference)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa phương tiện thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi nghiệp vụ — Xe đang có lịch đặt Pending hoặc lịch dịch vụ chưa hoàn thành trong tương lai"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phương tiện với ID đã cho"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực — Cần Bearer Token")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID của xe cần xóa", required = true)
            @PathVariable("vehicleId") Long vehicleId) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện thao tác xóa xe!");
        }
        
        boolean isAdminOrStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                               a.getAuthority().equals("ROLE_MANAGER") || 
                               a.getAuthority().equals("ROLE_CASHIER"));

        if (isAdminOrStaff) {
            vehicleService.deleteVehicle(vehicleId);
        } else {
            vehicleService.deleteVehicle(principal.getId(), vehicleId);
        }
        return ResponseEntity.noContent().build();
    }
}
