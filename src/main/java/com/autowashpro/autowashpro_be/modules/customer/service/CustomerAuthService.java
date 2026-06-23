package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.common.service.NotificationService;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.*;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
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
    private final StaffRepository staffRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpStoreService otpStoreService;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final SecurityTokenService securityTokenService;

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
        String email = MailService.normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();
        String phoneNumber = normalizePhone(request.getPhoneNumber());

        if (staffRepository.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username already taken");
        }
        if (customerRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already registered");
        }
        if (customerRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        if (customerRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Phone number already registered");
        }

        LoyaltyTier regularTier = loyaltyTierRepository.findByTierName("REGULAR")
                .or(() -> loyaltyTierRepository.findByTierName("MEMBER"))
                .orElseThrow(() -> new BadRequestException("Default tier not configured"));

        Customer customer = Customer.builder()
                .username(username)
                .email(email)
                .phoneNumber(phoneNumber)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(CustomerAuthProvider.EMAIL)
                .status(CustomerStatus.PENDING_ACTIVATION)
                .tier(regularTier)
                .build();
        customerRepository.save(customer);

        SecurityToken securityToken = securityTokenService.createToken(customer, SecurityTokenType.EMAIL_VERIFICATION);
        MailService.SendResult sendResult = mailService.sendRegistrationVerificationEmail(
                email, request.getFullName(), securityToken.getToken());

        if (!sendResult.success()) {
            throw new BadRequestException("Failed to send verification email: " + sendResult.message());
        }

        CustomerEmailRegisterResponse.CustomerEmailRegisterResponseBuilder builder = CustomerEmailRegisterResponse.builder()
                .message("Registration successful. Please check your email to activate your account.")
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

        Customer customer = securityToken.getCustomer();
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
        securityTokenService.markUsed(securityToken);

        return VerifyEmailTokenResponse.builder()
                .success(true)
                .message("Email verified successfully. Please login to continue.")
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerAuthResponse loginWithEmail(CustomerEmailLoginRequest request) {
        String email = MailService.normalizeEmail(request.getEmail());

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (customer.getAuthProvider() != CustomerAuthProvider.EMAIL) {
            throw new BadRequestException("This account uses phone login. Please sign in with your phone number.");
        }

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BadRequestException("Account not activated. Please verify your email first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(customer);
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
