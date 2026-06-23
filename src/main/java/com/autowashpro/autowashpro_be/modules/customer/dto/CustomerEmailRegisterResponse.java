package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerEmailRegisterResponse {

    private String message;
    private String email;
    private String mailMode;
    /** MOCK mode: URL xác thực để test không cần mở email */
    private String devActionUrl;
}
