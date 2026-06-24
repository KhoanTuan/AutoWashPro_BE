package com.autowashpro.autowashpro_be.modules.identity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Catalog quyền theo giai đoạn triển khai.
 * Phase 1: CRUD + Flow 1 (booking vận hành)
 * Phase 2: Flow 2 (thông báo) — seed sẵn, bật dần
 */
public final class PermissionCatalog {

    public static final int PHASE_CORE = 1;
    public static final int PHASE_NOTIFICATION = 2;

    public static final Set<String> SYSTEM_ROLE_NAMES = Set.of(
            "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_TECHNICIAN", "ROLE_CASHIER"
    );

    /** Roles whose permission matrix cannot be edited via API (always all enabled perms). */
    public static final Set<String> PERMISSION_LOCKED_ROLE_NAMES = Set.of("ROLE_ADMIN");

    /** Roles that cannot be deleted under any circumstance. */
    public static final Set<String> NON_DELETABLE_ROLE_NAMES = Set.of("ROLE_ADMIN");

    /** Permission codes for staff admin APIs (Phase A — granular CRUD). */
    public static final String READ_STAFF = "READ_STAFF";
    public static final String CREATE_UPDATE_STAFF = "CREATE_UPDATE_STAFF";
    public static final String DELETE_STAFF = "DELETE_STAFF";

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
            def("ASSIGN_ROLE", "Gán vai trò cho nhân viên", "Identity & RBAC", PHASE_CORE),
            def("MANAGE_ROLE", "Tạo / sửa / xóa vai trò", "Identity & RBAC", PHASE_CORE),
            def("CONFIG_RBAC_MATRIX", "Cấu hình ma trận phân quyền", "Identity & RBAC", PHASE_CORE),

            // ── Phase 1: Customer CRM ──
            def("VIEW_CUSTOMER_PROFILE", "Xem hồ sơ khách hàng", "Customer CRM", PHASE_CORE),
            def("MANAGE_CUSTOMER_STATUS", "Quản lý khách hàng (CRUD)", "Customer CRM", PHASE_CORE),

            // ── Phase 1: Booking Flow 1 ──
            def("CREATE_WALK_IN_BOOKING", "Tạo & quản lý booking", "Booking (Flow 1)", PHASE_CORE),
            def("CASHIER_CHECKIN", "Thu tiền tại quầy (Checkout)", "Booking (Flow 1)", PHASE_CORE),
            def("VIEW_SLOT_AVAILABILITY", "Xem khung giờ trống", "Booking (Flow 1)", PHASE_CORE),
            def("VIEW_TECH_QUEUE", "Xem hàng đợi kỹ thuật", "Operations (Flow 1)", PHASE_CORE),
            def("TASK_CHECKLIST", "Nhận / bắt đầu / hoàn thành ca rửa", "Operations (Flow 1)", PHASE_CORE),
            def("MONITOR_REALTIME_QUEUE", "Giám sát queue realtime", "Operations (Flow 1)", PHASE_CORE),

            // ── Phase 2: Notifications Flow 2 ──
            def("SEND_BOOKING_NOTIFICATION", "Gửi thông báo đặt lịch thành công", "Notification (Flow 2)", PHASE_NOTIFICATION, false),
            def("SEND_INCIDENT_ALERT", "Gửi cảnh báo sự cố bãi", "Notification (Flow 2)", PHASE_NOTIFICATION, false),
            def("VIEW_NOTIFICATION_LOG", "Xem nhật ký thông báo", "Notification (Flow 2)", PHASE_NOTIFICATION, false)
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
