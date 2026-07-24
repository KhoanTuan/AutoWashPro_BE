package com.autowashpro.autowashpro_be.modules.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Catalog quyền theo giai đoạn triển khai (Chuẩn hóa theo mô hình Admin - Manager - Cashier).
 * Phase 1: CRUD + Flow 1 (booking vận hành, POS, khách hàng, dịch vụ, khoang bãi, cài đặt)
 * Phase 2: Flow 2 (thông báo) — seed sẵn, bật dần
 */
public final class PermissionCatalog {

    public static final int PHASE_CORE = 1;
    public static final int PHASE_NOTIFICATION = 2;

    public static final Set<String> SYSTEM_ROLE_NAMES = Set.of(
            "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER"
    );

    /** Roles whose permission matrix cannot be edited via API (always all enabled perms). */
    public static final Set<String> PERMISSION_LOCKED_ROLE_NAMES = Set.of("ROLE_ADMIN");

    /** Roles that cannot be deleted under any circumstance. */
    public static final Set<String> NON_DELETABLE_ROLE_NAMES = Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER");

    // 1. Đặt lịch & Vận hành quầy POS
    public static final String VIEW_BOOKINGS = "VIEW_BOOKINGS";
    public static final String UPDATE_BOOKING_STATUS = "UPDATE_BOOKING_STATUS";
    public static final String CHECKIN_LATE = "CHECKIN_LATE";
    public static final String CHECKOUT_BOOKING = "CHECKOUT_BOOKING";
    public static final String LOCK_SLOT = "LOCK_SLOT";

    // 2. Dịch vụ & Khung giờ
    public static final String VIEW_SERVICES = "VIEW_SERVICES";
    public static final String MANAGE_SERVICES = "MANAGE_SERVICES";
    public static final String MANAGE_SLOTS = "MANAGE_SLOTS";

    // 3. Khách hàng CRM
    public static final String VIEW_CUSTOMERS = "VIEW_CUSTOMERS";
    public static final String MANAGE_CUSTOMER_STATUS = "MANAGE_CUSTOMER_STATUS";

    // 4. Admin Dashboard
    public static final String VIEW_DASHBOARD = "VIEW_DASHBOARD";

    // 5. Khuyến mãi & Tặng quà
    public static final String VIEW_PROMOTIONS = "VIEW_PROMOTIONS";
    public static final String MANAGE_PROMOTIONS = "MANAGE_PROMOTIONS";
    public static final String GRANT_PROMOTIONS = "GRANT_PROMOTIONS";

    // 6. Đánh giá & CSKH
    public static final String VIEW_FEEDBACKS = "VIEW_FEEDBACKS";
    public static final String RESOLVE_FEEDBACK = "RESOLVE_FEEDBACK";

    // 7. Thông báo trạm
    public static final String VIEW_NOTIFICATIONS = "VIEW_NOTIFICATIONS";

    // 8. Role & RBAC Matrix
    public static final String CONFIG_RBAC_MATRIX = "CONFIG_RBAC_MATRIX";

    @Getter
    @RequiredArgsConstructor
    public static class Definition {
        private final String code;
        private final String label;
        private final String moduleGroup;
        private final int phase;
        private final boolean enabled;
    }

    public static final List<Definition> ALL = List.of(
            // ── Luồng E2E-1: Vận hành Quầy POS & Đặt lịch ──
            def(VIEW_BOOKINGS, "Xem danh sách & chi tiết đơn đặt lịch", "Luồng E2E-1: Vận hành Quầy POS & Đặt lịch", PHASE_CORE),
            def(UPDATE_BOOKING_STATUS, "Cập nhật trạng thái đơn đặt lịch (Xác nhận, Đang rửa, Xong)", "Luồng E2E-1: Vận hành Quầy POS & Đặt lịch", PHASE_CORE),
            def(CHECKIN_LATE, "Xác nhận Check-in trễ giờ tại quầy", "Luồng E2E-1: Vận hành Quầy POS & Đặt lịch", PHASE_CORE),
            def(CHECKOUT_BOOKING, "Thanh toán hóa đơn & hoàn tất đơn tại quầy", "Luồng E2E-1: Vận hành Quầy POS & Đặt lịch", PHASE_CORE),
            def(LOCK_SLOT, "Khóa / Mở khóa thủ công mốc giờ rửa xe", "Luồng E2E-1: Vận hành Quầy POS & Đặt lịch", PHASE_CORE),

            // ── Luồng E2E-2: Khách hàng CRM ──
            def(VIEW_CUSTOMERS, "Tra cứu thông tin khách hàng & lịch sử điểm tích lũy", "Luồng E2E-2: Khách hàng CRM", PHASE_CORE),
            def(MANAGE_CUSTOMER_STATUS, "Khóa / Mở khóa trạng thái hoạt động tài khoản khách hàng", "Luồng E2E-2: Khách hàng CRM", PHASE_CORE),

            // ── Luồng E2E-3: Khuyến mãi & Direct Gifting ──
            def(VIEW_PROMOTIONS, "Xem danh sách chiến dịch & KPI khuyến mãi", "Luồng E2E-3: Khuyến mãi & Direct Gifting", PHASE_CORE),
            def(MANAGE_PROMOTIONS, "Tạo mới, kích hoạt / xóa chiến dịch Voucher", "Luồng E2E-3: Khuyến mãi & Direct Gifting", PHASE_CORE),
            def(GRANT_PROMOTIONS, "Xem trước tệp đối tượng & Tặng voucher trực tiếp", "Luồng E2E-3: Khuyến mãi & Direct Gifting", PHASE_CORE),
            def(VIEW_FEEDBACKS, "Xem danh sách đánh giá từ khách hàng", "Luồng E2E-3: Khuyến mãi & Direct Gifting", PHASE_CORE),
            def(RESOLVE_FEEDBACK, "Xử lý khiếu nại & Phát voucher đền bù", "Luồng E2E-3: Khuyến mãi & Direct Gifting", PHASE_CORE),

            // ── Luồng E2E-4: Admin Dashboard & Analytics ──
            def(VIEW_DASHBOARD, "Xem báo cáo KPI, Biểu đồ Doanh thu & Slot", "Luồng E2E-4: Dashboard & Analytics", PHASE_CORE),

            // ── Cấu hình Hệ thống & RBAC Matrix ──
            def(VIEW_SERVICES, "Xem bảng giá gói rửa & mốc giờ làm việc", "Cấu hình Hệ thống & RBAC", PHASE_CORE),
            def(MANAGE_SERVICES, "Thêm mới, sửa giá & Bật/Tắt gói dịch vụ", "Cấu hình Hệ thống & RBAC", PHASE_CORE),
            def(MANAGE_SLOTS, "Quản lý khung giờ, công suất & lịch đóng cửa", "Cấu hình Hệ thống & RBAC", PHASE_CORE),
            def(VIEW_NOTIFICATIONS, "Xem & đánh dấu đã đọc thông báo trạm", "Cấu hình Hệ thống & RBAC", PHASE_CORE),
            def(CONFIG_RBAC_MATRIX, "Cấu hình Ma trận phân quyền RBAC hệ thống", "Cấu hình Hệ thống & RBAC", PHASE_CORE)
    );

    public static List<Definition> phase1() {
        return ALL.stream().filter(d -> d.phase == PHASE_CORE).toList();
    }

    public static Optional<Definition> findByCode(String code) {
        return ALL.stream().filter(d -> d.code.equals(code)).findFirst();
    }

    public static Set<String> phase1Codes() {
        return phase1().stream().map(Definition::getCode).collect(java.util.stream.Collectors.toSet());
    }

    private static Definition def(String code, String label, String group, int phase) {
        return new Definition(code, label, group, phase, true);
    }

    private static Definition def(String code, String label, String group, int phase, boolean enabled) {
        return new Definition(code, label, group, phase, enabled);
    }

    private PermissionCatalog() {
    }
}
