package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyEmailTokenResponse {

    private boolean success;
    private String message;
}
