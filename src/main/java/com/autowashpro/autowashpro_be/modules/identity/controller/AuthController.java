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
                    **Staff** — dùng `loginId` hoặc `username` + `password`:
                    ```json
                    { "loginId": "tech01", "password": "Tech@123" }
                    ```

                    **Customer** — chỉ dùng `phoneNumber` + `password` (không dùng loginId):
                    ```json
                    { "phoneNumber": "0902000001", "password": "Customer@123" }
                    ```

                    Hoặc đăng nhập khách qua `POST /api/v1/customer/auth/login`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công", content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sai thông tin đăng nhập"),
            @ApiResponse(responseCode = "400", description = "Thiếu/sai định danh đăng nhập hoặc tài khoản staff bị khóa")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
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
