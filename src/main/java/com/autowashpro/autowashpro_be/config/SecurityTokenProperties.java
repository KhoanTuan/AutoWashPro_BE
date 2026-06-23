package com.autowashpro.autowashpro_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security-token")
public class SecurityTokenProperties {

    private int emailVerificationMinutes = 30;
    private int passwordResetMinutes = 10;
}
