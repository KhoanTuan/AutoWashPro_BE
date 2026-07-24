package com.autowashpro.autowashpro_be;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AutoWashProBeApplication {

    @PostConstruct
    public void init() {
        // Cố định múi giờ toàn bộ hệ thống Backend về giờ Việt Nam (GMT+7)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AutoWashProBeApplication.class, args);
    }

}
