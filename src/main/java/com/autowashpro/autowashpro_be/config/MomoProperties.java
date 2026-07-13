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
    private String partnerCode = "MOMO";
    private String accessKey = "F8BBA842ECF85";
    private String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String redirectUrl = "http://localhost:5173/admin/bookings";
    private String ipnUrl = "http://localhost:8080/api/v1/callback/momo/ipn";
    private String requestType = "captureWallet";
    private String lang = "vi";
}
