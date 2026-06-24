package com.autowashpro.autowashpro_be.common.service;

import com.autowashpro.autowashpro_be.config.SpeedSmsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOtpService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SpeedSmsProperties speedSmsProperties;
    private final RestClient restClient = RestClient.create();

    public void sendOtp(String phoneNumber, String otp) {
        if (isSpeedSmsConfigured()) {
            try {
                sendViaSpeedSms(phoneNumber, otp);
                return;
            } catch (Exception ex) {
                log.error("[SMS/SpeedSMS] Failed to send OTP to {}: {}", phoneNumber, ex.getMessage());
            }
        } else if (speedSmsProperties.isEnabled()) {
            log.warn("[SMS] SpeedSMS enabled but access-token missing — falling back to console log");
        }
        log.info("[SMS/mock] OTP for {} — code: {} (valid 2 minutes)", phoneNumber, otp);
    }

    private boolean isSpeedSmsConfigured() {
        return speedSmsProperties.isEnabled()
                && StringUtils.hasText(speedSmsProperties.getAccessToken());
    }

    private void sendViaSpeedSms(String phoneNumber, String otp) throws Exception {
        String normalizedPhone = normalizeVnPhone(phoneNumber);
        String content = "Ma xac thuc AutoWash Pro: " + otp + ". Hieu luc 2 phut.";

        Map<String, Object> body = Map.of(
                "to", List.of(normalizedPhone),
                "content", content,
                "sms_type", speedSmsProperties.getSmsType(),
                "sender", speedSmsProperties.getSender() != null ? speedSmsProperties.getSender() : ""
        );

        String credentials = speedSmsProperties.getAccessToken() + ":x";
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String responseBody = restClient.post()
                .uri(speedSmsProperties.getApiUrl())
                .header("Authorization", "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode json = OBJECT_MAPPER.readTree(responseBody);
        String status = json.path("status").asText("");
        if (!"success".equalsIgnoreCase(status)) {
            String message = json.path("message").asText(json.toString());
            throw new IllegalStateException("SpeedSMS rejected request: " + message);
        }
        log.info("[SMS/SpeedSMS] OTP sent to {}", normalizedPhone);
    }

    /** SpeedSMS chấp nhận 09x / 01x hoặc 849x */
    static String normalizeVnPhone(String phone) {
        String digits = phone.replaceAll("\\s+", "");
        if (digits.startsWith("+84")) {
            return "0" + digits.substring(3);
        }
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }
}
