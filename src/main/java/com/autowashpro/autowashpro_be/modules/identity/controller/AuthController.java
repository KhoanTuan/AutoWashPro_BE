package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.modules.identity.dto.ChangePasswordRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.JwtResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.LoginRequest;
import com.autowashpro.autowashpro_be.modules.identity.service.AuthService;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "02 - Staff Auth", description = "Xác thực nhân sự nội bộ — cổng ẩn `/internal-login`")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Đăng nhập nhân viên nội bộ",
            description = """
                    **Frontend route:** `/login-internal`

                    - Định danh bằng `username` (không phải SĐT)
                    - Trả về JWT kèm `roles`, `permissions`, `forceChangePassword`
                    - Nếu `forceChangePassword = true`: FE khóa sidebar, hiện overlay buộc đổi mật khẩu
                    - Lưu `accessToken` vào localStorage, gắn header `Authorization: Bearer {token}` cho các API sau
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sai username hoặc password"),
            @ApiResponse(responseCode = "400", description = "Tài khoản bị khóa (INACTIVE)")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Lấy thông tin nhân viên đang đăng nhập",
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
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Đổi mật khẩu nhân viên",
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
