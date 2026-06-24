package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.modules.customer.dto.VerifyEmailTokenResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.identity.dto.ChangePasswordRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.JwtResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.LoginRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.StaffForgotPasswordResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.StaffResetPasswordTokenRequest;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.security.CustomUserDetailsService;
import com.autowashpro.autowashpro_be.security.JwtTokenProvider;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final StaffService staffService;
    private final PasswordEncoder passwordEncoder;

    private static final String STAFF_REDIRECT = "/admin/dashboard";
    private static final String CUSTOMER_REDIRECT = "/customer/dashboard";

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        String loginId = request.resolveLoginId();
        if (loginId == null || loginId.isBlank()) {
            throw new BadRequestException("Login ID is required");
        }

        Optional<Staff> staffOpt = staffRepository.findByLoginId(loginId);
        if (staffOpt.isPresent()) {
            return loginStaff(staffOpt.get(), request.getPassword());
        }

        Optional<Customer> customerOpt = findCustomerByLoginId(loginId);
        if (customerOpt.isPresent()) {
            return loginCustomer(customerOpt.get(), request.getPassword());
        }

        throw new BadCredentialsException("Invalid credentials");
    }

    private Optional<Customer> findCustomerByLoginId(String loginId) {
        if (loginId.contains("@")) {
            Optional<Customer> byEmail = customerRepository.findByEmail(MailService.normalizeEmail(loginId));
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }

        String phone = normalizePhone(loginId);
        if (phone.matches("^0\\d{9,10}$")) {
            Optional<Customer> byPhone = customerRepository.findByPhoneNumber(phone);
            if (byPhone.isPresent()) {
                return byPhone;
            }
        }

        return customerRepository.findByUsername(loginId.trim());
    }

    private JwtResponse loginStaff(Staff staff, String rawPassword) {
        if (staff.isDeleted()) {
            throw new BadRequestException("Account has been removed. Contact administrator.");
        }
        if (!passwordEncoder.matches(rawPassword, staff.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (staff.getStatus() == StaffStatus.PENDING_ACTIVATION) {
            throw new BadRequestException("Account not activated. Please verify your email first.");
        }
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive. Contact administrator.");
        }
        JwtResponse response = buildJwtResponse(staff);
        response.setUserType(UserPrincipal.UserType.STAFF.name());
        response.setRedirectUrl(STAFF_REDIRECT);
        return response;
    }

    private JwtResponse loginCustomer(Customer customer, String rawPassword) {
        if (customer.getStatus() == CustomerStatus.PENDING_ACTIVATION) {
            throw new BadRequestException("Account not activated. Please verify your email first.");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }
        if (!passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String subject = resolveCustomerTokenSubject(customer);
        String token = jwtTokenProvider.generateCustomerToken(customer.getCustomerId(), subject);
        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userType(UserPrincipal.UserType.CUSTOMER.name())
                .redirectUrl(CUSTOMER_REDIRECT)
                .customerId(customer.getCustomerId())
                .username(customer.getUsername())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .tierName(customer.getTier().getTierName())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .roles(List.of("ROLE_CUSTOMER"))
                .build();
    }

    private static String resolveCustomerTokenSubject(Customer customer) {
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isBlank()) {
            return customer.getPhoneNumber();
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail();
        }
        return customer.getUsername();
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

    @Transactional
    public JwtResponse changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        staffService.changePassword(principal.getId(), request);

        Staff staff = staffRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Staff not found"));

        return buildJwtResponse(staff);
    }

    @Transactional
    public VerifyEmailTokenResponse verifyStaffEmail(String token) {
        return staffService.verifyStaffEmail(token);
    }

    @Transactional
    public StaffForgotPasswordResponse requestStaffPasswordResetByEmail(String email) {
        return staffService.requestPasswordResetByEmail(email);
    }

    @Transactional
    public void resetStaffPasswordByToken(StaffResetPasswordTokenRequest request) {
        staffService.resetPasswordByToken(request);
    }

    @Transactional(readOnly = true)
    public JwtResponse getCurrentStaffProfile(UserPrincipal principal) {
        Staff staff = staffRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Staff not found"));

        List<String> permissions = principal.getPermissionCodes();
        List<String> roles = principal.getRoleCodes();

        return JwtResponse.builder()
                .staffId(staff.getStaffId())
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .roles(roles)
                .permissions(permissions)
                .forceChangePassword(Boolean.TRUE.equals(staff.getRequirePasswordChange()))
                .build();
    }

    private JwtResponse buildJwtResponse(Staff staff) {
        UserPrincipal principal = userDetailsService.toStaffPrincipal(staff);
        List<String> permissions = principal.getPermissionCodes();
        List<String> roles = principal.getRoleCodes();

        boolean forceChange = Boolean.TRUE.equals(staff.getRequirePasswordChange());
        String token = jwtTokenProvider.generateStaffToken(
                staff.getStaffId(), staff.getUsername(), roles, permissions, forceChange);

        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .staffId(staff.getStaffId())
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .roles(roles)
                .permissions(permissions)
                .forceChangePassword(forceChange)
                .build();
    }
}
