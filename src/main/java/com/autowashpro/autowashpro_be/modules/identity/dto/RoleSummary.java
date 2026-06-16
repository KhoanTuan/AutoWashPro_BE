package com.autowashpro.autowashpro_be.modules.identity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleSummary {
    private Integer roleId;
    private String roleName;
}
