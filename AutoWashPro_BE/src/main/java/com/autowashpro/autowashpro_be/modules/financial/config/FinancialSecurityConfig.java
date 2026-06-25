package com.autowashpro.autowashpro_be.modules.financial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class FinancialSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain momoWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/financial/webhook/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
