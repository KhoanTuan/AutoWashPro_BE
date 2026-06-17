package com.autowashpro.autowashpro_be.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void sendStaffWelcomeEmail(String email, String username, String temporaryPassword) {
        log.info("[EMAIL] Staff account created — to: {}, username: {}, tempPassword: {}",
                email, username, temporaryPassword);
    }

    public void sendPasswordResetEmail(String email, String username, String temporaryPassword) {
        log.info("[EMAIL] Staff password reset — to: {}, username: {}, tempPassword: {}",
                email, username, temporaryPassword);
    }

    public void sendOtpSms(String phoneNumber, String otp) {
        log.info("[SMS] OTP for {} — code: {} (valid 2 minutes)", phoneNumber, otp);
    }
}
