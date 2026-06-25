package com.autowashpro.autowashpro_be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {
    private String url = "http://localhost:5173";
}
