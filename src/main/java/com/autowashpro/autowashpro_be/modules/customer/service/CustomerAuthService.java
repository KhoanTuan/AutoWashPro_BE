package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.NotificationService;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.security.JwtTokenProvider;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpStoreService otpStoreService;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final NotificationService notificationService;

    public RegisterOtpSentResponse requestRegister(CustomerRegisterRequest request) {
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        pendingRegistrationStore.save(request.getPhoneNumber(), request.getFullName(), passwordHash);

        String otp = otpStoreService.generateAndStore(request.getPhoneNumber());
        notificationService.sendOtpSms(request.getPhoneNumber(), otp);

        return RegisterOtpSentResponse.builder()
                .message("OTP sent to your phone number")
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds((int) pendingRegistrationStore.getTtlSeconds())
                .build();
    }

    public RegisterOtpSentResponse resendRegisterOtp(CustomerResendRegisterOtpRequest request) {
        return resendRegisterOtp(request.getPhoneNumber());
    }

    public RegisterOtpSentResponse resendRegisterOtp(String phoneNumber) {
        if (!pendingRegistrationStore.hasPending(phoneNumber)) {
            throw new BadRequestException("Registration session expired. Please register again.");
        }
        if (customerRepository.existsByPhoneNumber(phoneNumber)) {
            pendingRegistrationStore.remove(phoneNumber);
            throw new BadRequestException("Phone number already registered");
        }

        String otp = otpStoreService.generateAndStore(phoneNumber);
        notificationService.sendOtpSms(phoneNumber, otp);

        return RegisterOtpSentResponse.builder()
                .message("OTP sent to your phone number")
                .phoneNumber(phoneNumber)
                .expiresInSeconds((int) pendingRegistrationStore.getTtlSeconds())
                .build();
    }

    @Transactional
    public void verifyRegister(CustomerRegisterVerifyRequest request) {
        if (!otpStoreService.verify(request.getPhoneNumber(), request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        PendingRegistrationStore.PendingRegistration pending = pendingRegistrationStore.find(request.getPhoneNumber())
                .orElseThrow(() -> new BadRequestException("Registration session expired. Please register again."));

        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            pendingRegistrationStore.remove(request.getPhoneNumber());
            throw new BadRequestException("Phone number already registered");
        }

        LoyaltyTier regularTier = loyaltyTierRepository.findByTierName("REGULAR")
                .or(() -> loyaltyTierRepository.findByTierName("MEMBER"))
                .orElseThrow(() -> new BadRequestException("Default tier not configured"));

        Customer customer = Customer.builder()
                .phoneNumber(request.getPhoneNumber())
                .fullName(pending.fullName())
                .passwordHash(pending.passwordHash())
                .tier(regularTier)
                .build();

        customerRepository.save(customer);
        pendingRegistrationStore.remove(request.getPhoneNumber());
    }

    @Transactional(readOnly = true)
    public CustomerAuthResponse login(CustomerLoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid phone number or password");
        }

        Customer customer = customerRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid phone number or password"));

        return buildAuthResponse(customer);
    }

    @Transactional(readOnly = true)
    public ForgotPasswordResponse forgotPassword(CustomerForgotPasswordRequest request) {
        Customer customer = customerRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not registered"));

        String otp = otpStoreService.generateAndStore(customer.getPhoneNumber());
        notificationService.sendOtpSms(customer.getPhoneNumber(), otp);

        return ForgotPasswordResponse.builder()
                .message("OTP sent to your phone number")
                .expiresInSeconds((int) otpStoreService.getTtlSeconds())
                .build();
    }

    @Transactional
    public void resetPassword(CustomerResetPasswordRequest request) {
        if (!otpStoreService.verify(request.getPhoneNumber(), request.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Customer customer = customerRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not registered"));

        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UserPrincipal principal) {
        Customer customer = customerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return CustomerProfileResponse.builder()
                .customerId(customer.getCustomerId())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .tierName(customer.getTier().getTierName())
                .visitCount(customer.getVisitCount())
                .totalSpending(customer.getTotalSpending())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .build();
    }

    private CustomerAuthResponse buildAuthResponse(Customer customer) {
        String token = jwtTokenProvider.generateCustomerToken(customer.getCustomerId(), customer.getPhoneNumber());
        return CustomerAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .customerId(customer.getCustomerId())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .tierName(customer.getTier().getTierName())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .build();
    }
}
