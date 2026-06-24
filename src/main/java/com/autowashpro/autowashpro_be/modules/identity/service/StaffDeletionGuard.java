package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Chặn xóa staff khi còn gán booking/task (Phase B).
 * Nếu bảng {@code task_checklist} chưa có (chưa merge module booking), chỉ kiểm tra {@code work_status = BUSY}.
 */
@Service
@RequiredArgsConstructor
public class StaffDeletionGuard {

    private final JdbcTemplate jdbcTemplate;

    public void ensureSafeToDelete(Staff staff, boolean hardDelete) {
        if (staff.getWorkStatus() == StaffWorkStatus.BUSY) {
            throw new BadRequestException(
                    "Cannot delete staff while work status is BUSY (active job in progress)");
        }

        if (!taskChecklistTableExists()) {
            return;
        }

        Long activeAssignments = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM task_checklist tc
                WHERE tc.technician_id = ?
                  AND tc.status IN ('NOT_STARTED', 'PROCESSING')
                """, Long.class, staff.getStaffId());

        if (activeAssignments != null && activeAssignments > 0) {
            String mode = hardDelete ? "permanently delete" : "delete";
            throw new BadRequestException(
                    "Cannot " + mode + " staff: still assigned to "
                            + activeAssignments + " active booking(s)");
        }
    }

    private boolean taskChecklistTableExists() {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'task_checklist'
                )
                """, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
