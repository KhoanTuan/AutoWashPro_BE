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
    public static final String TAG_08_OPERATIONS_QUEUE = "08 - Operations Queue";
    public static final String TAG_09_TECHNICIAN_TABLET = "09 - Technician Tablet";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoWash Pro — API Reference (FE)")
                        .version("1.1.0")
                        .description("""
                                Tài liệu API cho **Frontend Quoc** (`AutoWashPro_FE`).

                                **Swagger UI:** `http://localhost:8080/swagger-ui.html`

                                ---

                                ## Cách đọc tài liệu
                                - Các nhóm API được đánh số **01 → 07** theo luồng tích hợp FE.
                                - Trong mỗi nhóm, endpoint sắp xếp theo `operationId` (01, 02, 03…).
                                - Admin API cần header: `Authorization: Bearer {accessToken}` (login qua **02 - Omni Auth**).

                                ---

                                ## Bảng map API ↔ Trang FE

                                | Tag | Trang FE | Service FE (gợi ý) |
                                |-----|----------|-------------------|
                                | 01 | `/register`, `/login` (customer) | `authService.js` |
                                | 02 | `/login`, `/login-internal` | `authService.js` |
                                | 03 | `/admin/staff` | `staffService.js` |
                                | 04 | `/admin/customers` | `customerService.js` |
                                | 05 | `/admin/bookings`, Quick Booking modal | `bookingService.js` |
                                | 06 | `/admin/roles` — CRUD role | `roleService.js` |
                                | 07 | `/admin/roles` — ma trận permission | `roleService.js` |
                                | 08 | `/admin/operations/queue` — shop-floor monitor | `queueService.js` |
                                | 09 | Technician tablet bays | `technicianService.js` |

                                ---

                                ## Flow 1 — Booking (tag 05)
                                ```
                                POST /admin/bookings          → PENDING_PAYMENT + UNPAID
                                POST /admin/bookings/{id}/pay → PAID (CASHIER_CHECKIN) + queue check-in
                                PATCH .../assign-staff        → gán KTV
                                PATCH .../accept              → CONFIRMED
                                PATCH .../start               → PROCESSING
                                PATCH .../complete            → COMPLETED
                                ```

                                ## Flow 1 — Operations (tag 08–09)
                                ```
                                GET  /operations/queue              → double-queue layout
                                POST /operations/queue/{id}/check-in  → enqueue after payment
                                POST /operations/technician/tasks/{id}/claim → tablet claim
                                PATCH /operations/technician/checklist-items/{id}/complete
                                ```
                                Các bước sau **pay** đều yêu cầu `paymentStatus = PAID`.

                                ---

                                ## Phân trang (chung)
                                - Query: `page` (0-based), `size` (mặc định 10)
                                - Response: `{ content[], page, size, totalElements, totalPages }`

                                ---

                                ## Tài khoản demo
                                | Vai trò | Định danh đăng nhập | Password |
                                |---------|---------------------|----------|
                                | Admin / Manager / Technician | `loginId`: `admin`, `manager`, `tech01` | `Admin@123`, `Manager@123`, `Tech@123` |
                                | Customer | `phoneNumber`: `0902000001` | `Customer@123` |

                                **Staff:** `{ "loginId": "tech01", "password": "Tech@123" }`  
                                **Customer:** `{ "phoneNumber": "0902000001", "password": "Customer@123" }`
                                """)
                        .contact(new Contact().name("AutoWash Pro Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ))
                .tags(List.of(
                        tag(TAG_01_CUSTOMER_AUTH,
                                "Cổng công khai — đăng ký OTP, login, quên mật khẩu khách hàng. Không cần token."),
                        tag(TAG_02_OMNI_AUTH,
                                "Omni-Login staff + customer qua `POST /api/v1/auth/login`. Staff: `/login-internal`."),
                        tag(TAG_03_ADMIN_STAFF,
                                "CRUD nhân sự — `/admin/staff`. Permission: `MANAGE_STAFF`, `ASSIGN_ROLE`."),
                        tag(TAG_04_ADMIN_CUSTOMER,
                                "CRUD khách hàng — `/admin/customers`. Permission: `VIEW_CUSTOMER_PROFILE`, `MANAGE_CUSTOMER_STATUS`."),
                        tag(TAG_05_ADMIN_BOOKING,
                                "Flow 1 booking — `/admin/bookings` + Quick Booking. Catalog → CRUD → Pay → Assign → Accept → Start → Complete."),
                        tag(TAG_06_ROLES,
                                "CRUD vai trò — `/admin/roles`. Permission: `MANAGE_ROLE`. Dropdown role: `GET /roles` (cần `ASSIGN_ROLE`)."),
                        tag(TAG_07_PERMISSION_MATRIX,
                                "Ma trận phân quyền — `/admin/roles`. Permission: `CONFIG_RBAC_MATRIX`. Phase 1 = enabled, Phase 2 = sắp có."),
                        tag(TAG_08_OPERATIONS_QUEUE,
                                "Shop-floor queue monitor — `/operations/queue`. Permission: `VIEW_TECH_QUEUE`, `MONITOR_REALTIME_QUEUE`."),
                        tag(TAG_09_TECHNICIAN_TABLET,
                                "Technician tablet — `/operations/technician`. Permission: `VIEW_TECH_QUEUE`, `TASK_CHECKLIST`.")
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
