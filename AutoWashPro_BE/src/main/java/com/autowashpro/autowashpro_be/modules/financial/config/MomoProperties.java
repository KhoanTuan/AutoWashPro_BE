package com.autowashpro.autowashpro_be.modules.financial.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.momo")
public class MomoProperties {

    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String ipnUrl;
    private String redirectUrl;
    private String requestType = "captureWallet";
}
