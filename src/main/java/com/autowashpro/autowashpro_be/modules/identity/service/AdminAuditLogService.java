package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.AdminAuditLogResponse;
import com.autowashpro.autowashpro_be.modules.identity.entity.AdminAuditLog;
import com.autowashpro.autowashpro_be.modules.identity.repository.AdminAuditLogRepository;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    public static final String TARGET_STAFF = "STAFF";

    public static final String ACTION_STAFF_SOFT_DELETE = "STAFF_SOFT_DELETE";
    public static final String ACTION_STAFF_HARD_DELETE = "STAFF_HARD_DELETE";
    public static final String ACTION_STAFF_RESTORE = "STAFF_RESTORE";
    public static final String ACTION_STAFF_RESEND_ACTIVATION = "STAFF_RESEND_ACTIVATION";

    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public void logStaffAction(String action, Long staffId, String detail) {
        UserPrincipal actor = currentPrincipal();
        auditLogRepository.save(AdminAuditLog.builder()
                .actorStaffId(actor != null ? actor.getId() : null)
                .actorUsername(actor != null ? actor.getUsername() : "system")
                .action(action)
                .targetType(TARGET_STAFF)
                .targetId(staffId)
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> listStaffAuditLogs(int page, int size) {
        Page<AdminAuditLog> result = auditLogRepository.findByTargetTypeOrderByCreatedAtDesc(
                TARGET_STAFF,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<AdminAuditLogResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<AdminAuditLogResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .auditId(log.getAuditId())
                .actorStaffId(log.getActorStaffId())
                .actorUsername(log.getActorUsername())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private UserPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
