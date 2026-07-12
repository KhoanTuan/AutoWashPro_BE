package com.autowashpro.autowashpro_be.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST Template configuration for HTTP client operations.
 * Configured for communication with external payment gateways like MoMo.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates and configures a RestTemplate bean for HTTP requests.
     * 
     * @param builder RestTemplateBuilder for fluent configuration
     * @return Configured RestTemplate with timeout settings
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
