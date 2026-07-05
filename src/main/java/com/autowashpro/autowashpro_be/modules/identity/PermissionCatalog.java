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
    public static final Set<String> NON_DELETABLE_ROLE_NAMES = Set.of("ROLE_ADMIN");

    /** Permission codes for staff admin APIs & RBAC */
    public static final String READ_STAFF = "READ_STAFF";
    public static final String CREATE_UPDATE_STAFF = "CREATE_UPDATE_STAFF";
    public static final String DELETE_STAFF = "DELETE_STAFF";
    public static final String ASSIGN_ROLE = "ASSIGN_ROLE";
    public static final String MANAGE_ROLE = "MANAGE_ROLE";
    public static final String CONFIG_RBAC_MATRIX = "CONFIG_RBAC_MATRIX";

    /** Permission codes for Customer CRM & Loyalty */
    public static final String VIEW_CUSTOMER_PROFILE = "VIEW_CUSTOMER_PROFILE";
    public static final String MANAGE_CUSTOMER_STATUS = "MANAGE_CUSTOMER_STATUS";
    public static final String MANAGE_LOYALTY_CONFIG = "MANAGE_LOYALTY_CONFIG";

    /** Permission codes for Booking & Operations (POS, Queue, Wash Progress) */
    public static final String CREATE_WALK_IN_BOOKING = "CREATE_WALK_IN_BOOKING";
    public static final String CASHIER_CHECKIN = "CASHIER_CHECKIN";
    public static final String CANCEL_BOOKING = "CANCEL_BOOKING";
    public static final String VIEW_SLOT_AVAILABILITY = "VIEW_SLOT_AVAILABILITY";
    public static final String VIEW_STATION_QUEUE = "VIEW_STATION_QUEUE";
    public static final String MANAGE_WASH_PROGRESS = "MANAGE_WASH_PROGRESS";
    public static final String MONITOR_REALTIME_QUEUE = "MONITOR_REALTIME_QUEUE";

    /** Permission codes for Services, Slots & Station Settings */
    public static final String MANAGE_SERVICE_CATALOG = "MANAGE_SERVICE_CATALOG";
    public static final String MANAGE_SLOT_CONFIG = "MANAGE_SLOT_CONFIG";
    public static final String MANAGE_STATION_SETTINGS = "MANAGE_STATION_SETTINGS";
    public static final String VIEW_DASHBOARD_STATS = "VIEW_DASHBOARD_STATS";

    /** Permission codes for Notifications (Phase 2) */
    public static final String SEND_BOOKING_NOTIFICATION = "SEND_BOOKING_NOTIFICATION";
    public static final String SEND_INCIDENT_ALERT = "SEND_INCIDENT_ALERT";
    public static final String VIEW_NOTIFICATION_LOG = "VIEW_NOTIFICATION_LOG";

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
            // ── Phase 1: Identity & RBAC ──
            def(READ_STAFF, "Xem danh sách / chi tiết nhân viên", "Identity & RBAC", PHASE_CORE),
            def(CREATE_UPDATE_STAFF, "Tạo / sửa / khóa nhân viên", "Identity & RBAC", PHASE_CORE),
            def(DELETE_STAFF, "Xóa nhân viên (soft/hard)", "Identity & RBAC", PHASE_CORE),
            def(ASSIGN_ROLE, "Gán vai trò cho nhân viên", "Identity & RBAC", PHASE_CORE),
            def(MANAGE_ROLE, "Tạo / sửa / xóa vai trò", "Identity & RBAC", PHASE_CORE),
            def(CONFIG_RBAC_MATRIX, "Cấu hình ma trận phân quyền", "Identity & RBAC", PHASE_CORE),

            // ── Phase 1: Customer CRM & Loyalty ──
            def(VIEW_CUSTOMER_PROFILE, "Xem hồ sơ khách hàng & tích điểm", "Customer CRM", PHASE_CORE),
            def(MANAGE_CUSTOMER_STATUS, "Quản lý khách hàng (Khóa / Mở khóa)", "Customer CRM", PHASE_CORE),
            def(MANAGE_LOYALTY_CONFIG, "Cấu hình chính sách Tích điểm & Hạng thành viên", "Customer CRM", PHASE_CORE),

            // ── Phase 1: Booking Flow 1 & Operations ──
            def(CREATE_WALK_IN_BOOKING, "Tạo & tiếp nhận đơn trực tiếp tại trạm", "Booking & POS", PHASE_CORE),
            def(CASHIER_CHECKIN, "Thu tiền tại quầy (Checkout / Hóa đơn)", "Booking & POS", PHASE_CORE),
            def(CANCEL_BOOKING, "Hủy đặt lịch rửa xe & giải phóng khoang", "Booking & POS", PHASE_CORE),
            def(VIEW_SLOT_AVAILABILITY, "Xem sơ đồ khung giờ & khoang trống", "Booking & POS", PHASE_CORE),
            def(VIEW_STATION_QUEUE, "Xem danh sách xe đang rửa tại trạm", "Operations", PHASE_CORE),
            def(MANAGE_WASH_PROGRESS, "Bắt đầu rửa & xác nhận hoàn thành ca rửa", "Operations", PHASE_CORE),
            def(MONITOR_REALTIME_QUEUE, "Giám sát màn hình tổng Dashboard realtime", "Operations", PHASE_CORE),
            def(VIEW_DASHBOARD_STATS, "Xem báo cáo thống kê doanh thu & hoạt động", "Operations", PHASE_CORE),

            // ── Phase 1: Services, Slots & Station Settings ──
            def(MANAGE_SERVICE_CATALOG, "Quản lý gói dịch vụ & Bảng giá theo xe", "Service & Pricing", PHASE_CORE),
            def(MANAGE_SLOT_CONFIG, "Cấu hình khoang rửa & Lịch làm việc", "Service & Pricing", PHASE_CORE),
            def(MANAGE_STATION_SETTINGS, "Cài đặt thông tin chung của Trạm rửa xe", "System Settings", PHASE_CORE),

            // ── Phase 2: Notifications Flow 2 ──
            def(SEND_BOOKING_NOTIFICATION, "Gửi thông báo đặt lịch thành công", "Notification (Flow 2)", PHASE_NOTIFICATION, false),
            def(SEND_INCIDENT_ALERT, "Gửi cảnh báo sự cố bãi", "Notification (Flow 2)", PHASE_NOTIFICATION, false),
            def(VIEW_NOTIFICATION_LOG, "Xem nhật ký thông báo", "Notification (Flow 2)", PHASE_NOTIFICATION, false)
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
