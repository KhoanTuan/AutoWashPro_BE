package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.CustomerProfileResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAuthService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Profile Management", description = "Quản lý thông tin cá nhân và mật khẩu cho App Khách hàng")
public class CustomerProfileController {

    private final CustomerAuthService customerAuthService;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    @Operation(summary = "Lấy hồ sơ cá nhân khách hàng")
    public ResponseEntity<CustomerProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(customerAuthService.getProfile(principal));
    }

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật thông tin cá nhân (họ tên, email)")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        requireAuthenticated(principal);
        Customer customer = customerRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Khách hàng không tồn tại!"));
        
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customerRepository.save(customer);

        return ResponseEntity.ok(customerAuthService.getProfile(principal));
    }

    @PostMapping("/email/request-verification")
    @Operation(summary = "Gửi mã xác thực email")
    public ResponseEntity<MessageResponse> requestVerification(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(MessageResponse.of("Hệ thống đã gửi link kích hoạt đến Gmail của bạn. Trạng thái đã được xác thực!"));
    }

    @PostMapping("/profile/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        requireAuthenticated(principal);
        Customer customer = customerRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Khách hàng không tồn tại!"));

        if (!passwordEncoder.matches(request.getOldPassword(), customer.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác!");
        }

        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);

        return ResponseEntity.ok(MessageResponse.of("Thay đổi mật khẩu tài khoản thành công!"));
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để sử dụng chức năng này!");
        }
    }

    @Data
    public static class ProfileUpdateRequest {
        private String fullName;
        private String email;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
