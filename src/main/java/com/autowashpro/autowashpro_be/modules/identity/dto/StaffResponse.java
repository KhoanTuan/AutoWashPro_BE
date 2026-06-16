package com.autowashpro.autowashpro_be.modules.identity.dto;

import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StaffResponse {
    private Long staffId;
    private String username;
    private String fullName;
    private StaffStatus status;
    private List<RoleSummary> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
