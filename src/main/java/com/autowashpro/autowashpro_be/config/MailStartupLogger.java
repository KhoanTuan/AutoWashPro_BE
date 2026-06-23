package com.autowashpro.autowashpro_be.config;

import com.autowashpro.autowashpro_be.common.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailStartupLogger {

    private final MailService mailService;
    private final GmailMailProperties gmailProperties;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @EventListener(ApplicationReadyEvent.class)
    public void logMailStatus() {
        log.info("=== Mail config === mode={} | gmailReady={} | smtpUser={}",
                mailService.getMailMode(),
                mailService.isGmailConfigured(),
                mask(smtpUsername));

        if ("GMAIL".equalsIgnoreCase(mailService.getMailMode()) && !mailService.isGmailConfigured()) {
            log.warn("app.mail.mode=GMAIL but SMTP credentials missing in application-local.yaml");
            return;
        }

        if ("GMAIL".equalsIgnoreCase(mailService.getMailMode()) && mailSender instanceof JavaMailSenderImpl impl) {
            try {
                impl.testConnection();
                log.info("=== SMTP auth test: OK ===");
            } catch (Exception ex) {
                log.error("=== SMTP auth test: FAILED — {} ===", ex.getMessage());
            }
        }
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "(not set)";
        }
        if (value.length() <= 6) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
