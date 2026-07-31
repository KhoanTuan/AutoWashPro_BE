package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.common.service.NotificationService;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.*;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.security.JwtTokenProvider;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpStoreService otpStoreService;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final SecurityTokenService securityTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .authProvider(CustomerAuthProvider.PHONE)
                .status(CustomerStatus.ACTIVE)
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

        ensureActiveForLogin(customer);

        return buildAuthResponse(customer);
    }

    @Transactional
    public CustomerEmailRegisterResponse registerWithEmail(CustomerEmailRegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Username is required and cannot be empty");
        }
        String email = MailService.normalizeEmail(request.getEmail());
        String phoneNumber = normalizePhone(request.getPhoneNumber());
        String username = request.getUsername().trim();

        if (staffRepository.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        // Clean up any old unverified test account with PENDING_ACTIVATION from earlier tests
        customerRepository.findByUsername(username).ifPresent(c -> {
            if (c.getStatus() == CustomerStatus.PENDING_ACTIVATION) {
                customerRepository.delete(c);
                customerRepository.flush();
            } else {
                throw new BadRequestException("Username already registered");
            }
        });
        customerRepository.findByEmail(email).ifPresent(c -> {
            if (c.getStatus() == CustomerStatus.PENDING_ACTIVATION) {
                customerRepository.delete(c);
                customerRepository.flush();
            } else {
                throw new BadRequestException("Email already registered");
            }
        });
        customerRepository.findByPhoneNumber(phoneNumber).ifPresent(c -> {
            if (c.getStatus() == CustomerStatus.PENDING_ACTIVATION) {
                customerRepository.delete(c);
                customerRepository.flush();
            } else {
                throw new BadRequestException("Phone number already registered");
            }
        });

        // Store registration data temporarily in security token payload WITHOUT creating customer entity
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "email", email,
                    "phoneNumber", phoneNumber,
                    "fullName", request.getFullName().trim(),
                    "passwordHash", passwordEncoder.encode(request.getPassword())
            ));
        } catch (Exception e) {
            throw new BadRequestException("Failed to process registration data");
        }

        SecurityToken securityToken = securityTokenService.createRegistrationToken(payload, SecurityTokenType.EMAIL_VERIFICATION);
        MailService.SendResult sendResult = mailService.sendRegistrationVerificationEmail(
                email, request.getFullName(), securityToken.getToken());

        if (!sendResult.success()) {
            throw new BadRequestException("Failed to send verification email: " + sendResult.message());
        }

        CustomerEmailRegisterResponse.CustomerEmailRegisterResponseBuilder builder = CustomerEmailRegisterResponse.builder()
                .message("Registration initiated. Please check your email and confirm to create your account.")
                .email(email)
                .mailMode(sendResult.mode());

        if ("MOCK".equalsIgnoreCase(sendResult.mode())) {
            builder.devActionUrl(sendResult.actionUrl());
        }

        return builder.build();
    }

    @Transactional
    public VerifyEmailTokenResponse verifyEmail(String tokenValue) {
        SecurityToken securityToken = securityTokenService.requireValidToken(
                tokenValue, SecurityTokenType.EMAIL_VERIFICATION);

        if (securityToken.getPayload() != null) {
            try {
                Map<String, String> data = objectMapper.readValue(securityToken.getPayload(), Map.class);
                String username = data.get("username");
                String email = data.get("email");
                String phoneNumber = data.get("phoneNumber");

                if (customerRepository.existsByUsername(username) || customerRepository.existsByEmail(email) || customerRepository.existsByPhoneNumber(phoneNumber)) {
                    throw new BadRequestException("Account already registered or verified");
                }

                LoyaltyTier regularTier = loyaltyTierRepository.findByTierName("REGULAR")
                        .or(() -> loyaltyTierRepository.findByTierName("MEMBER"))
                        .orElseThrow(() -> new BadRequestException("Default tier not configured"));

                Customer customer = Customer.builder()
                        .username(username)
                        .email(email)
                        .phoneNumber(phoneNumber)
                        .fullName(data.get("fullName"))
                        .passwordHash(data.get("passwordHash"))
                        .authProvider(CustomerAuthProvider.EMAIL)
                        .status(CustomerStatus.ACTIVE)
                        .tier(regularTier)
                        .build();

                customerRepository.save(customer);
            } catch (Exception e) {
                if (e instanceof BadRequestException) throw (BadRequestException) e;
                throw new BadRequestException("Invalid registration data in token: " + e.getMessage());
            }
        } else if (securityToken.getCustomer() != null) {
            Customer customer = securityToken.getCustomer();
            customer.setStatus(CustomerStatus.ACTIVE);
            customerRepository.save(customer);
        } else {
            throw new BadRequestException("Invalid token: missing account information");
        }

        securityTokenService.markUsed(securityToken);

        return VerifyEmailTokenResponse.builder()
                .success(true)
                .message("Email verified successfully. Your account has been created and activated.")
                .build();
    }


    @Transactional
    public void requestPasswordResetByEmail(String email) {
        String normalized = MailService.normalizeEmail(email);
        customerRepository.findByEmail(normalized).ifPresent(customer -> {
            if (customer.getAuthProvider() != CustomerAuthProvider.EMAIL) {
                return;
            }
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                return;
            }

            SecurityToken securityToken = securityTokenService.createToken(customer, SecurityTokenType.PASSWORD_RESET);
            mailService.sendPasswordResetEmail(customer.getEmail(), customer.getFullName(), securityToken.getToken());
        });
    }

    @Transactional
    public void resetPasswordByToken(CustomerResetPasswordTokenRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        SecurityToken securityToken = securityTokenService.requireValidToken(
                request.getToken(), SecurityTokenType.PASSWORD_RESET);

        Customer customer = securityToken.getCustomer();
        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
        securityTokenService.markUsed(securityToken);
    }


    @Transactional(readOnly = true)
    public ForgotPasswordResponse forgotPassword(CustomerForgotPasswordRequest request) {
        Customer customer = customerRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not registered"));

        if (customer.getAuthProvider() == CustomerAuthProvider.EMAIL) {
            throw new BadRequestException("Use email reset for this account");
        }

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

        List<VehicleResponse> vehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customer.getCustomerId())
                .stream()
                .map(v -> VehicleResponse.builder()
                        .vehicleId(v.getVehicleId())
                        .customerId(customer.getCustomerId())
                        .licensePlate(v.getLicensePlate())
                        .model(v.getModel())
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();

        String tierName = customer.getTier() != null && customer.getTier().getTierName() != null ? customer.getTier().getTierName() : "REGULAR";
        String dispName = switch (tierName.toUpperCase()) {
            case "REGULAR", "MEMBER" -> "Member";
            case "SILVER" -> "Silver";
            case "GOLD" -> "Gold";
            case "PLATINUM" -> "Platinum";
            default -> tierName;
        };
        int windowDays = customer.getTier() != null && customer.getTier().getBookingWindowDays() != null ? customer.getTier().getBookingWindowDays() : 7;

        return CustomerProfileResponse.builder()
                .customerId(customer.getCustomerId())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .tierName(tierName)
                .tierDisplayName(dispName)
                .bookingWindowDays(windowDays)
                .visitCount(customer.getVisitCount())
                .totalSpending(customer.getTotalSpending())
                .tierSpending(customer.getTierSpending() != null ? customer.getTierSpending() : BigDecimal.ZERO)
                .loyaltyPoints(customer.getLoyaltyPoints())
                .vehicles(vehicles)
                .build();
    }

    @Transactional
    public CustomerProfileResponse updateProfile(UserPrincipal principal, com.autowashpro.autowashpro_be.modules.customer.dto.UpdateCustomerProfileRequest request) {
        Customer customer = customerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            customer.setFullName(request.getFullName().trim());
        }

        customerRepository.save(customer);
        log.info("Updated profile for customer {}: fullName={}", customer.getCustomerId(), customer.getFullName());
        return getProfile(principal);
    }

    private CustomerAuthResponse buildAuthResponse(Customer customer) {
        String subject = customer.getPhoneNumber() != null && !customer.getPhoneNumber().isBlank()
                ? customer.getPhoneNumber()
                : (customer.getEmail() != null ? customer.getEmail() : customer.getUsername());
        String token = jwtTokenProvider.generateCustomerToken(customer.getCustomerId(), subject);
        return CustomerAuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .customerId(customer.getCustomerId())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .tierName(customer.getTier().getTierName())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .build();
    }

    private static String normalizePhone(String input) {
        String digits = input.replaceAll("\\s+", "");
        if (digits.startsWith("+84")) {
            return "0" + digits.substring(3);
        }
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    private void ensureActiveForLogin(Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }
    }
}
