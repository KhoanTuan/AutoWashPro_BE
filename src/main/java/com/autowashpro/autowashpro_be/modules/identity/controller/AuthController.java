package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.VerifyEmailTokenResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.ChangePasswordRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.JwtResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.LoginRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.StaffEmailForgotPasswordRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.StaffForgotPasswordResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.StaffResetPasswordTokenRequest;
import com.autowashpro.autowashpro_be.modules.identity.service.AuthService;
import com.autowashpro.autowashpro_be.common.openapi.ApiHidden;
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

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_02_OMNI_AUTH;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = TAG_02_OMNI_AUTH, description = "Omni-Login + phiên staff")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            operationId = "02-01-login",
            summary = "[PUBLIC] Omni-Login",
            description = """
                    Một ô `loginId` (username staff / username khách / email / SĐT) + password.
                    - Staff trước → redirect `/admin/dashboard`
                    - Khách hàng (SĐT, email hoặc username) → redirect `/customer/dashboard`
                    - Tài khoản email (staff/customer) phải đã xác thực (`ACTIVE`)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sai thông tin đăng nhập"),
            @ApiResponse(responseCode = "400", description = "Tài khoản staff bị khóa hoặc thiếu loginId")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/staff/verify-email")
    @SecurityRequirements
    @Operation(
            operationId = "02-04-staff-verify-email",
            summary = "[PUBLIC] Kích hoạt tài khoản nhân viên qua email",
            description = "FE route: `/staff/verify-email?token=...` — chuyển staff từ `PENDING_ACTIVATION` sang `ACTIVE`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kích hoạt thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    public ResponseEntity<VerifyEmailTokenResponse> verifyStaffEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyStaffEmail(token));
    }

    @PostMapping("/staff/forgot-password")
    @SecurityRequirements
    @Operation(
            operationId = "02-05-staff-forgot-password",
            summary = "[PUBLIC] Quên mật khẩu nhân viên — gửi link email",
            description = "Luôn trả 200 (không lộ email có tồn tại hay không). Chỉ tài khoản staff `ACTIVE` mới nhận email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Yêu cầu đã được xử lý"),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ")
    })
    public ResponseEntity<StaffForgotPasswordResponse> forgotStaffPassword(
            @Valid @RequestBody StaffEmailForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.requestStaffPasswordResetByEmail(request.getEmail()));
    }

    @PostMapping("/staff/reset-password")
    @SecurityRequirements
    @Operation(
            operationId = "02-06-staff-reset-password",
            summary = "[PUBLIC] Đặt lại mật khẩu nhân viên qua token email",
            description = "FE route: `/staff/reset-password?token=...`"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt lại mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc mật khẩu không khớp")
    })
    public ResponseEntity<MessageResponse> resetStaffPassword(
            @Valid @RequestBody StaffResetPasswordTokenRequest request
    ) {
        authService.resetStaffPasswordByToken(request);
        return ResponseEntity.ok(MessageResponse.of(
                "Password reset successfully. Please login with your new password."));
    }

    @GetMapping("/me")
    @ApiHidden
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            operationId = "02-02-me",
            summary = "[READ] Profile staff đang đăng nhập",
            description = """
                    Dùng sau khi reload trang để khôi phục session staff.

                    - Cần token staff hợp lệ
                    - Không trả lại `accessToken` (chỉ profile + roles + permissions)
                    - Trả `forceChangePassword` để FE kiểm tra trạng thái đổi mật khẩu
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy profile thành công"),
            @ApiResponse(responseCode = "403", description = "Token không phải staff hoặc chưa đăng nhập")
    })
    public ResponseEntity<JwtResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.STAFF) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(authService.getCurrentStaffProfile(principal));
    }

    @PutMapping("/change-password")
    @ApiHidden
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            operationId = "02-03-change-password",
            summary = "[UPDATE] Đổi mật khẩu staff",
            description = """
                    **Khi nào gọi:** Nhân viên bị `forceChangePassword = true` sau khi Admin tạo/reset tài khoản.

                    - Gửi mật khẩu hiện tại (thường là mật khẩu tạm `Welcome@2026` hoặc `AutoWash@2026`)
                    - Sau khi đổi thành công: trả JWT mới với `forceChangePassword = false`
                    - FE cập nhật token mới và mở khóa sidebar
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công, trả JWT mới"),
            @ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại không đúng"),
            @ApiResponse(responseCode = "403", description = "Chưa đăng nhập staff")
    })
    public ResponseEntity<JwtResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.STAFF) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(authService.changePassword(principal, request));
    }
}
