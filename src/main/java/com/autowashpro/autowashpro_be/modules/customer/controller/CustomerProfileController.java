package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.modules.customer.dto.CustomerProfileResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.UpdateCustomerProfileRequest;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAuthService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_01_CUSTOMER_AUTH;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = TAG_01_CUSTOMER_AUTH)
public class CustomerProfileController {

    private final CustomerAuthService customerAuthService;

    @GetMapping("/profile")
    @Operation(
            summary = "[READ] Xem thông tin cá nhân khách hàng",
            description = "Cần Bearer token customer. Trả về thông tin profile, hạng VIP, điểm thưởng và xe."
    )
    public ResponseEntity<CustomerProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerAuthService.getProfile(principal));
    }

    @PutMapping("/profile")
    @Operation(
            summary = "[UPDATE] Cập nhật thông tin cá nhân khách hàng (Họ và tên)",
            description = "Cần Bearer token customer. Cho phép khách hàng tự cập nhật Họ và tên hiển thị."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật Họ và tên thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Token không phải customer")
    })
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerAuthService.updateProfile(principal, request));
    }
}
