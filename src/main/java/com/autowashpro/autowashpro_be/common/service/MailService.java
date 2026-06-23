package com.autowashpro.autowashpro_be.common.service;

import com.autowashpro.autowashpro_be.config.FrontendProperties;
import com.autowashpro.autowashpro_be.config.GmailMailProperties;
import com.autowashpro.autowashpro_be.config.SecurityTokenProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final GmailMailProperties gmailProperties;
    private final FrontendProperties frontendProperties;
    private final SecurityTokenProperties securityTokenProperties;

    @Value("${app.mail.mode:MOCK}")
    private String mailMode;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public String getMailMode() {
        return mailMode != null ? mailMode.toUpperCase() : "MOCK";
    }

    public boolean isGmailConfigured() {
        return StringUtils.hasText(resolveUsername()) && StringUtils.hasText(resolvePassword());
    }

    public SendResult sendRegistrationVerificationEmail(String toEmail, String fullName, String token) {
        String verificationUrl = frontendProperties.getUrl() + "/verify-email?token=" + token;
        return sendTemplateEmail(
                toEmail,
                "AutoWash Pro — Xác nhận địa chỉ email",
                "registration-verify",
                Map.of(
                        "fullName", fullName,
                        "verificationUrl", verificationUrl,
                        "brandName", "AutoWash Pro"
                ),
                verificationUrl
        );
    }

    public SendResult sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String resetUrl = frontendProperties.getUrl() + "/reset-password?token=" + token;
        return sendTemplateEmail(
                toEmail,
                "AutoWash Pro — Đặt lại mật khẩu",
                "password-reset",
                Map.of(
                        "fullName", fullName != null ? fullName : "bạn",
                        "resetUrl", resetUrl,
                        "brandName", "AutoWash Pro",
                        "expiryMinutes", String.valueOf(securityTokenProperties.getPasswordResetMinutes())
                ),
                resetUrl
        );
    }

    private SendResult sendTemplateEmail(String toEmail, String subject, String templateName,
                                         Map<String, Object> variables, String actionUrl) {
        String normalizedEmail = normalizeEmail(toEmail);
        String html = processTemplate(templateName, variables);

        if ("GMAIL".equalsIgnoreCase(getMailMode()) && isGmailConfigured()) {
            String fromEmail = resolveUsername();
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, gmailProperties.getFromName());
                helper.setTo(normalizedEmail);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
                log.info("[Mail/GMAIL] {} | from={} | to={}", templateName, fromEmail, normalizedEmail);
                return new SendResult(true, "GMAIL", "Email sent via Gmail SMTP", actionUrl, fromEmail, normalizedEmail);
            } catch (Exception ex) {
                log.error("[Mail/GMAIL] Failed to send {} to {}: {}", templateName, normalizedEmail, ex.getMessage(), ex);
                return new SendResult(false, "GMAIL", ex.getMessage(), actionUrl, fromEmail, normalizedEmail);
            }
        }

        if ("GMAIL".equalsIgnoreCase(getMailMode())) {
            log.warn("[Mail] GMAIL mode but missing SMTP credentials — fallback to MOCK");
        }

        log.info("[Mail/MOCK] To: {} | Subject: {} | Action URL: {}", normalizedEmail, subject, actionUrl);
        return new SendResult(true, "MOCK", "Logged action URL to console (MOCK mode)", actionUrl, null, normalizedEmail);
    }

    private String resolveUsername() {
        return StringUtils.hasText(gmailProperties.getUsername())
                ? gmailProperties.getUsername()
                : smtpUsername;
    }

    private String resolvePassword() {
        return StringUtils.hasText(gmailProperties.getAppPassword())
                ? gmailProperties.getAppPassword()
                : smtpPassword;
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record SendResult(boolean success, String mode, String message, String actionUrl,
                             String fromEmail, String sentTo) {
    }
}
