package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
import com.autowashpro.autowashpro_be.common.openapi.ApiHidden;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_01_CUSTOMER_AUTH;

@RestController
@RequestMapping("/api/v1/customer/auth")
@RequiredArgsConstructor
@Tag(name = TAG_01_CUSTOMER_AUTH, description = "Đăng ký / login / quên MK khách hàng bằng **email** (Security Token)")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register/request")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-01-register-request",
            summary = "[PUBLIC] Đăng ký bước 1 — gửi OTP",
            description = "Nhận họ tên, SĐT, mật khẩu → gửi OTP 6 số qua SpeedSMS. OTP hết hạn sau 120s."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP đã gửi"),
            @ApiResponse(responseCode = "400", description = "SĐT đã tồn tại hoặc dữ liệu không hợp lệ")
    })
    public ResponseEntity<RegisterOtpSentResponse> requestRegister(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.ok(customerAuthService.requestRegister(request));
    }

    @PostMapping("/register/verify")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-02-register-verify",
            summary = "[PUBLIC] Đăng ký bước 2 — xác nhận OTP",
            description = "Gửi SĐT + OTP 6 số. Hợp lệ → tạo tài khoản, không trả JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "OTP sai, hết hạn hoặc phiên đăng ký hết hạn")
    })
    public ResponseEntity<MessageResponse> verifyRegister(@Valid @RequestBody CustomerRegisterVerifyRequest request) {
        customerAuthService.verifyRegister(request);
        return ResponseEntity.ok(MessageResponse.of("Registration successful. Please login."));
    }

    @PostMapping("/register/resend-otp")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-03-register-resend-otp",
            summary = "[PUBLIC] Gửi lại OTP đăng ký",
            description = "Gửi lại OTP khi phiên đăng ký tạm còn hiệu lực (120s)."
    )
    public ResponseEntity<RegisterOtpSentResponse> resendRegisterOtp(
            @Valid @RequestBody CustomerResendRegisterOtpRequest request
    ) {
        return ResponseEntity.ok(customerAuthService.resendRegisterOtp(request));
    }

    @PostMapping("/login")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-04-login",
            summary = "[PUBLIC] Đăng nhập khách hàng",
            description = "SĐT + mật khẩu → trả JWT (`accessToken`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "Sai SĐT hoặc mật khẩu")
    })
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(customerAuthService.login(request));
    }

    @PostMapping("/forgot-password")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-05-forgot-password",
            summary = "[PUBLIC] Quên mật khẩu bước 1 — gửi OTP",
            description = "Nhập SĐT đã đăng ký → gửi OTP qua SpeedSMS (TTL 120s)."
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
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-06-reset-password",
            summary = "[PUBLIC] Quên mật khẩu bước 2 — đặt mật khẩu mới",
            description = "SĐT + OTP + mật khẩu mới. OTP sai/hết hạn → 400."
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
    @ApiHidden
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            operationId = "01-07-me",
            summary = "[READ] Hồ sơ khách hàng",
            description = "Cần Bearer token customer. Trả tier, điểm, lịch sử chi tiêu."
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

    // ── Email Security Token flow ─────────────────────────────────────

    @PostMapping("/email/register")
    @SecurityRequirements
    @Operation(
            operationId = "01-08-email-register",
            summary = "[PUBLIC] Đăng ký bằng email",
            description = "Tạo tài khoản `PENDING_ACTIVATION` (username, email, SĐT, mật khẩu) và gửi link xác thực qua email."
    )
    public ResponseEntity<CustomerEmailRegisterResponse> registerWithEmail(
            @Valid @RequestBody CustomerEmailRegisterRequest request
    ) {
        return ResponseEntity.ok(customerAuthService.registerWithEmail(request));
    }

    @GetMapping("/email/verify")
    @SecurityRequirements
    @Operation(
            operationId = "01-09-email-verify",
            summary = "[PUBLIC] Xác thực email qua token",
            description = "FE route: `/verify-email?token=...` — kích hoạt tài khoản → `ACTIVE`."
    )
    public ResponseEntity<VerifyEmailTokenResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(customerAuthService.verifyEmail(token));
    }

    @PostMapping("/email/login")
    @ApiHidden
    @SecurityRequirements
    @Operation(
            operationId = "01-10-email-login",
            summary = "[PUBLIC] Đăng nhập bằng email",
            description = "Đã gộp vào Omni-Login (`POST /api/v1/auth/login`). Endpoint này giữ tương thích ngược."
    )
    public ResponseEntity<CustomerAuthResponse> loginWithEmail(
            @Valid @RequestBody CustomerEmailLoginRequest request
    ) {
        return ResponseEntity.ok(customerAuthService.loginWithEmail(request));
    }

    @PostMapping("/email/forgot-password")
    @SecurityRequirements
    @Operation(
            operationId = "01-11-email-forgot-password",
            summary = "[PUBLIC] Quên mật khẩu — gửi link email",
            description = "Luôn trả 200 (không lộ email có tồn tại hay không)."
    )
    public ResponseEntity<MessageResponse> forgotPasswordByEmail(
            @Valid @RequestBody CustomerEmailForgotPasswordRequest request
    ) {
        customerAuthService.requestPasswordResetByEmail(request.getEmail());
        return ResponseEntity.ok(MessageResponse.of(
                "If the email exists and the account is active, a password reset link has been sent."));
    }

    @PostMapping("/email/reset-password")
    @SecurityRequirements
    @Operation(
            operationId = "01-12-email-reset-password",
            summary = "[PUBLIC] Đặt lại mật khẩu qua token email",
            description = "FE route: `/reset-password?token=...`"
    )
    public ResponseEntity<MessageResponse> resetPasswordByToken(
            @Valid @RequestBody CustomerResetPasswordTokenRequest request
    ) {
        customerAuthService.resetPasswordByToken(request);
        return ResponseEntity.ok(MessageResponse.of(
                "Password reset successfully. Please login with your new password."));
    }
}
