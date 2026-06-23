package com.autowashpro.autowashpro_be.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final SmsOtpService smsOtpService;

    public void sendStaffWelcomeEmail(String email, String username, String temporaryPassword) {
        log.info("[EMAIL] Staff account created — to: {}, username: {}, tempPassword: {}",
                email, username, temporaryPassword);
    }

    public void sendPasswordResetEmail(String email, String username, String temporaryPassword) {
        log.info("[EMAIL] Staff password reset — to: {}, username: {}, tempPassword: {}",
                email, username, temporaryPassword);
    }

    public void sendOtpSms(String phoneNumber, String otp) {
        smsOtpService.sendOtp(phoneNumber, otp);
    }
}
