package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAuthService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/auth")
@RequiredArgsConstructor
@Tag(name = "01 - Customer Auth", description = "Xác thực khách hàng — cổng công khai `/login-customer`")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            summary = "Đăng ký tài khoản khách hàng",
            description = """
                    **Frontend route:** `/register`

                    - Định danh bằng `phoneNumber` (10-11 số, bắt đầu bằng 0)
                    - Tự gán hạng `REGULAR`, điểm tích lũy = 0
                    - Trả JWT ngay sau đăng ký → FE lưu token và điều hướng `/customer/dashboard`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công, trả JWT"),
            @ApiResponse(responseCode = "400", description = "SĐT đã tồn tại hoặc dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomerAuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAuthService.register(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Đăng nhập khách hàng",
            description = """
                    **Frontend route:** `/login-customer`

                    - Đăng nhập bằng SĐT + mật khẩu
                    - Trả JWT → lưu `accessToken` vào localStorage
                    - Điều hướng vào zone `/customer/*`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "Sai SĐT hoặc mật khẩu")
    })
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(customerAuthService.login(request));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
            summary = "Bước 1 — Gửi OTP quên mật khẩu",
            description = """
                    **Luồng 2 bước (self-service):**

                    1. Khách nhập SĐT → gọi API này
                    2. Backend sinh OTP 6 số, lưu RAM (TTL 120 giây), gửi SMS (dev: xem log console)
                    3. FE hiển thị form nhập OTP + mật khẩu mới → gọi `POST /reset-password`

                    **Lưu ý FE:** Hiển thị countdown `expiresInSeconds` (120s) trên UI
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP đã gửi (hoặc SĐT hợp lệ)"),
            @ApiResponse(responseCode = "404", description = "SĐT chưa đăng ký")
    })
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody CustomerForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(customerAuthService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
            summary = "Bước 2 — Xác thực OTP và đặt mật khẩu mới",
            description = """
                    - Gửi cùng `phoneNumber` đã dùng ở bước 1
                    - `otp`: mã 6 số nhận qua SMS (dev: xem log `[SMS] OTP for ...`)
                    - OTP hết hạn sau 120 giây hoặc sai mã → 400
                    - Thành công → khách đăng nhập lại bằng mật khẩu mới
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "OTP sai hoặc hết hạn"),
            @ApiResponse(responseCode = "404", description = "SĐT không tồn tại")
    })
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody CustomerResetPasswordRequest request
    ) {
        customerAuthService.resetPassword(request);
        return ResponseEntity.ok(MessageResponse.of("Password reset successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Lấy hồ sơ khách hàng đang đăng nhập",
            description = """
                    Dùng để khôi phục session sau reload trang customer zone.

                    - Cần token customer (không dùng token staff)
                    - Trả tier, điểm tích lũy, lịch sử chi tiêu
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy profile thành công"),
            @ApiResponse(responseCode = "403", description = "Token không phải customer")
    })
    public ResponseEntity<CustomerProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerAuthService.getProfile(principal));
    }
}
