package com.autowashpro.autowashpro_be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.speedsms")
public class SpeedSmsProperties {

    private boolean enabled = false;
    private String accessToken;
    private String apiUrl = "https://api.speedsms.vn/index.php/sms/send";
    /** 2 = tin CSKH (OTP) */
    private int smsType = 2;
    /** Brandname đã đăng ký; để trống nếu chưa có */
    private String sender = "";
}
