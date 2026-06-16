package com.autowashpro.autowashpro_be.modules.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JwtResponse {
    private String accessToken;
    private String tokenType;
    private Long staffId;
    private String username;
    private String fullName;
    private List<String> roles;
    private List<String> permissions;
}
