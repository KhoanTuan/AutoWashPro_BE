package com.autowashpro.autowashpro_be.modules.financial.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    private String apiKey;
    private String model = "gemini-2.0-flash";
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta/models";
}
