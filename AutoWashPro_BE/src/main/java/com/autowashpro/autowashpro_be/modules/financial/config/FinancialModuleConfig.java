package com.autowashpro.autowashpro_be.modules.financial.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({MomoProperties.class, GeminiProperties.class})
public class FinancialModuleConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
