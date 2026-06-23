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

    /** Thứ tự tag trên Swagger UI — khớp số prefix 01–07 */
    public static final String TAG_01_CUSTOMER_AUTH = "01 - Customer Auth";
    public static final String TAG_02_OMNI_AUTH = "02 - Omni Auth";
    public static final String TAG_03_ADMIN_STAFF = "03 - Admin Staff";
    public static final String TAG_04_ADMIN_CUSTOMER = "04 - Admin Customer";
    public static final String TAG_05_ADMIN_BOOKING = "05 - Admin Booking";
    public static final String TAG_06_ROLES = "06 - Roles";
    public static final String TAG_07_PERMISSION_MATRIX = "07 - Permission Matrix";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoWash Pro — API Reference (Quoc2 + Email)")
                        .version("1.2.0")
                        .description("""
                                Tài liệu API **đang tích hợp** cho **Quoc2 FE** + **Customer Email Auth**.

                                API legacy (OTP/SĐT, staff profile, CRUD role…) vẫn chạy nhưng **ẩn khỏi Swagger**.
                                Bật lại: gỡ `@ApiHidden` trên method tương ứng.

                                **Swagger UI:** `http://localhost:8080/swagger-ui.html`

                                ---

                                ## Nhóm hiển thị

                                | Tag | Nội dung |
                                |-----|----------|
                                | 01 | Customer **email** — register, verify, login, forgot/reset |
                                | 02 | Omni-Login (`POST /auth/login`) |
                                | 03 | Admin Staff (Quoc2) |
                                | 04 | Admin Customer — list, create, update |
                                | 05 | Admin Booking Flow 1 (Quoc2) |
                                | 06 | Roles — danh sách (dropdown) |
                                | 07 | Permission Matrix (Quoc2) |

                                ---

                                ## Auth
                                - Admin/Staff: `POST /api/v1/auth/login` → Bearer token
                                - Customer email: tag **01** (`/customer/auth/email/*`)

                                ---

                                ## Tài khoản demo
                                | Vai trò | Login | Password |
                                |---------|-------|----------|
                                | Admin | `admin` | `Admin@123` |
                                | Customer (SĐT) | `0902000001` | `Customer@123` |
                                """)
                        .contact(new Contact().name("AutoWash Pro Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ))
                .tags(List.of(
                        tag(TAG_01_CUSTOMER_AUTH,
                                "Customer email auth — register, verify link, login, forgot/reset password."),
                        tag(TAG_02_OMNI_AUTH,
                                "Omni-Login — `POST /api/v1/auth/login` (Quoc2 `/login-internal`)."),
                        tag(TAG_03_ADMIN_STAFF,
                                "CRUD nhân sự — `/admin/staff`. Permission: `MANAGE_STAFF`, `ASSIGN_ROLE`."),
                        tag(TAG_04_ADMIN_CUSTOMER,
                                "CRUD khách hàng — `/admin/customers`. Permission: `VIEW_CUSTOMER_PROFILE`, `MANAGE_CUSTOMER_STATUS`."),
                        tag(TAG_05_ADMIN_BOOKING,
                                "Flow 1 booking — `/admin/bookings` + Quick Booking. Catalog → CRUD → Pay → Assign → Accept → Start → Complete."),
                        tag(TAG_06_ROLES,
                                "Danh sách Role — dropdown trên trang Roles (Quoc2)."),
                        tag(TAG_07_PERMISSION_MATRIX,
                                "Ma trận phân quyền — `/admin/roles`. Permission: `CONFIG_RBAC_MATRIX`. Phase 1 = enabled, Phase 2 = sắp có.")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .description("JWT từ Omni-Login hoặc Customer login. Format: `Bearer {accessToken}`")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
