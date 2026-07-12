package com.autowashpro.autowashpro_be.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.momo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MomoProperties {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String redirectUrl;
    private String ipnUrl;
    private String requestType = "captureWallet";
    private String lang = "vi";
}
