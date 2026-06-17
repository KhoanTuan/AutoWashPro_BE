package com.autowashpro.autowashpro_be.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoWash Pro — Module 1 API")
                        .version("1.0.0")
                        .description("""
                                Tài liệu API **Module 1: Identity & Auth** dành cho Frontend.

                                ## Phân luồng xác thực
                                | Cổng | Base path | Định danh | Token lưu ở |
                                |------|-----------|-----------|-------------|
                                | Khách hàng (Public) | `/api/v1/customer/auth` | Số điện thoại | `localStorage.accessToken` |
                                | Nhân sự nội bộ (Hidden) | `/api/v1/auth` | Username | `localStorage.accessToken` |

                                ## Cách gọi API có bảo vệ
                                1. Gọi API login tương ứng để lấy `accessToken`
                                2. Gắn header: `Authorization: Bearer {accessToken}`
                                3. Staff login trả thêm `forceChangePassword` — nếu `true`, FE phải chặn sidebar và buộc đổi mật khẩu qua `PUT /api/v1/auth/change-password`

                                ## Tài khoản demo (seed)
                                - Admin: `admin` / `Admin@123`
                                - Cashier: `cashier` / `Cashier@123`
                                - Technician: `technician` / `Tech@123`
                                """)
                        .contact(new Contact().name("AutoWash Pro Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ))
                .tags(List.of(
                        new Tag().name("01 - Customer Auth")
                                .description("Cổng công khai cho khách hàng: đăng ký, đăng nhập, quên/reset mật khẩu OTP. Không cần token."),
                        new Tag().name("02 - Staff Auth")
                                .description("Cổng ẩn nội bộ: đăng nhập nhân viên, xem profile, đổi mật khẩu bắt buộc."),
                        new Tag().name("03 - Admin Staff")
                                .description("Quản trị nhân sự: tạo tài khoản, reset mật khẩu, gán role. Yêu cầu token staff."),
                        new Tag().name("04 - RBAC Matrix")
                                .description("Ma trận phân quyền: xem roles/permissions, cập nhật quyền cho role. Dùng cho trang `/internal/admin/rbac`.")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .description("JWT token từ API login. Format: `Bearer {accessToken}`")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
