package com.autowashpro.autowashpro_be.modules.customer.entity;

public enum SecurityTokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    /** Khách được tạo lúc booking claim tài khoản: đặt mật khẩu + kích hoạt. */
    ACCOUNT_CLAIM,
    STAFF_ACCOUNT_ACTIVATION,
    STAFF_PASSWORD_RESET
}
