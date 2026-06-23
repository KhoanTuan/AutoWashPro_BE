package com.autowashpro.autowashpro_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.gmail")
public class GmailMailProperties {

    private String username = "";
    private String appPassword = "";
    private String fromName = "AutoWash Pro";
}
