package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.identity.dto.ChangePasswordRequest;
import com.autowashpro.autowashpro_be.modules.identity.dto.JwtResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.LoginRequest;
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
import java.util.stream.Collectors;

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
    private static final String TECHNICIAN_REDIRECT = "/technician/dashboard";
    private static final String CUSTOMER_REDIRECT = "/customer/dashboard";

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        if (request.hasStaffLogin() && request.hasCustomerLogin()) {
            throw new BadRequestException("Use phoneNumber for customer login or loginId for staff login, not both");
        }
        if (request.hasCustomerLogin()) {
            return loginCustomerByPhone(normalizePhone(request.getPhoneNumber().trim()), request.getPassword());
        }
        if (request.hasStaffLogin()) {
            String loginId = request.resolveLoginId();
            Staff staff = resolveStaff(loginId)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            return loginStaff(staff, request.getPassword(), loginId);
        }
        throw new BadRequestException("phoneNumber (customer) or loginId (staff) is required");
    }

    private JwtResponse loginCustomerByPhone(String phone, String rawPassword) {
        Customer customer = customerRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return loginCustomer(customer, rawPassword);
    }

    private Optional<Staff> resolveStaff(String loginId) {
        Optional<Staff> byLoginId = staffRepository.findByLoginId(loginId);
        if (byLoginId.isPresent()) {
            return byLoginId;
        }
        return staffRepository.findByPhoneNumber(normalizePhone(loginId));
    }

    private JwtResponse loginStaff(Staff staff, String rawPassword, String loginIdUsed) {
        if (!passwordEncoder.matches(rawPassword, staff.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive. Contact administrator.");
        }
        JwtResponse response = buildJwtResponse(staff, loginIdUsed);
        response.setUserType(UserPrincipal.UserType.STAFF.name());
        response.setRedirectUrl(resolveStaffRedirect(staff));
        return response;
    }

    private JwtResponse loginCustomer(Customer customer, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtTokenProvider.generateCustomerToken(customer.getCustomerId(), customer.getPhoneNumber());
        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userType(UserPrincipal.UserType.CUSTOMER.name())
                .redirectUrl(CUSTOMER_REDIRECT)
                .customerId(customer.getCustomerId())
                .phoneNumber(customer.getPhoneNumber())
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

    @Transactional
    public JwtResponse changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        staffService.changePassword(principal.getId(), request);

        Staff staff = staffRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Staff not found"));

        return buildJwtResponse(staff, staff.getUsername());
    }

    @Transactional(readOnly = true)
    public JwtResponse getCurrentStaffProfile(UserPrincipal principal) {
        Staff staff = staffRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Staff not found"));

        List<String> permissions = principal.getPermissionCodes();
        List<String> roles = staff.getRoles().stream()
                .map(r -> r.getRoleName())
                .sorted()
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .staffId(staff.getStaffId())
                .loginId(staff.getUsername())
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .roles(roles)
                .permissions(permissions)
                .forceChangePassword(Boolean.TRUE.equals(staff.getRequirePasswordChange()))
                .build();
    }

    private JwtResponse buildJwtResponse(Staff staff, String loginIdUsed) {
        UserPrincipal principal = userDetailsService.toStaffPrincipal(staff);
        List<String> permissions = principal.getPermissionCodes();
        List<String> roles = staff.getRoles().stream()
                .map(r -> r.getRoleName())
                .sorted()
                .collect(Collectors.toList());

        boolean forceChange = Boolean.TRUE.equals(staff.getRequirePasswordChange());
        String token = jwtTokenProvider.generateStaffToken(
                staff.getStaffId(), staff.getUsername(), permissions, forceChange);

        return JwtResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .staffId(staff.getStaffId())
                .loginId(loginIdUsed != null ? loginIdUsed : staff.getUsername())
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .roles(roles)
                .permissions(permissions)
                .forceChangePassword(forceChange)
                .build();
    }

    private String resolveStaffRedirect(Staff staff) {
        boolean isTechnician = staff.getRoles().stream()
                .anyMatch(role -> "ROLE_TECHNICIAN".equals(role.getRoleName()));
        if (isTechnician) {
            return TECHNICIAN_REDIRECT;
        }
        return STAFF_REDIRECT;
    }
}
