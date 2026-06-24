package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.common.service.NotificationService;
import com.autowashpro.autowashpro_be.modules.customer.dto.VerifyEmailTokenResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityToken;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityTokenType;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.SecurityTokenRepository;
import com.autowashpro.autowashpro_be.modules.customer.service.SecurityTokenService;
import com.autowashpro.autowashpro_be.modules.identity.StaffConstants;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.RoleRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tầng business logic cho quản trị nhân sự — validation nghiệp vụ, transaction, DB, gửi email.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper mapper;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final SecurityTokenService securityTokenService;
    private final SecurityTokenRepository securityTokenRepository;

    private static final String DELETE_SUFFIX_MARKER = "_deleted_";

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> listStaff(String status, String keyword, boolean includeDeleted, int page, int size) {
        StaffStatus staffStatus = status != null && !status.isBlank()
                ? StaffStatus.valueOf(status.toUpperCase())
                : null;

        Page<Staff> result = staffRepository.search(
                staffStatus,
                keyword,
                includeDeleted,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<StaffResponse> content = result.getContent().stream()
                .map(mapper::toStaffResponse)
                .toList();

        return PageResponse.<StaffResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public StaffResponse getById(Long id) {
        Staff staff = staffRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        return mapper.toStaffResponse(staff);
    }

    @Transactional(readOnly = true)
    public StaffSummaryStatsResponse getSummaryStats() {
        List<Staff> activeStaff = staffRepository.findByStatus(StaffStatus.ACTIVE);
        if (activeStaff.isEmpty()) {
            return StaffSummaryStatsResponse.builder()
                    .totalActiveStaff(0)
                    .avgEfficiency(0)
                    .teamScore(0)
                    .onBreakNow(0)
                    .offDutyNow(0)
                    .build();
        }

        double avgEfficiency = activeStaff.stream()
                .mapToDouble(s -> s.getPerformanceKpi() != null ? s.getPerformanceKpi() : 0)
                .average()
                .orElse(0);

        double teamScore = activeStaff.stream()
                .mapToDouble(s -> s.getServiceRating() != null ? s.getServiceRating() : 0)
                .average()
                .orElse(0);

        long onBreak = activeStaff.stream()
                .filter(s -> s.getWorkStatus() == StaffWorkStatus.ON_BREAK)
                .count();

        long offDuty = activeStaff.stream()
                .filter(s -> s.getWorkStatus() == StaffWorkStatus.OFF)
                .count();

        return StaffSummaryStatsResponse.builder()
                .totalActiveStaff(activeStaff.size())
                .avgEfficiency(Math.round(avgEfficiency * 10.0) / 10.0)
                .teamScore(Math.round(teamScore * 100.0) / 100.0)
                .onBreakNow(onBreak)
                .offDutyNow(offDuty)
                .build();
    }

    @Transactional
    public StaffResponse updateStaff(Long id, UpdateStaffRequest request) {
        Staff staff = requireActiveStaff(id);

        if (!staff.getEmail().equalsIgnoreCase(request.getEmail())
                && staffRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String phone = normalizePhone(request.getPhoneNumber());
        if (phone != null && !phone.isBlank()) {
            staffRepository.findByPhoneNumber(phone).ifPresent(existing -> {
                if (!existing.getStaffId().equals(id)) {
                    throw new BadRequestException("Phone number already exists");
                }
            });
        }

        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPhoneNumber(phone);

        if (request.getRoleIds() != null) {
            Set<Role> roles = resolveRoles(request.getRoleIds());
            validateRoleAssignment(roles);
            staff.setRoles(roles);
        }

        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse updateWorkStatus(Long id, UpdateStaffWorkStatusRequest request) {
        Staff staff = requireActiveStaff(id);
        staff.setWorkStatus(request.getWorkStatus());
        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public CreateStaffResponse createStaff(CreateStaffRequest request) {
        String username = request.getUsername().trim();
        String email = MailService.normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhoneNumber());

        validateUniqueCredentials(username, email, phone, null);
        Set<Role> roles = resolveRoles(request.getRoleIds());
        validateRoleAssignment(roles);

        Staff staff = Staff.builder()
                .username(username)
                .email(email)
                .phoneNumber(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .requirePasswordChange(false)
                .status(StaffStatus.PENDING_ACTIVATION)
                .workStatus(StaffWorkStatus.IDLE)
                .performanceKpi(0.0)
                .totalJobsCompleted(0)
                .serviceRating(5.0)
                .roles(roles)
                .build();

        staff = staffRepository.save(staff);

        SecurityToken securityToken = securityTokenService.createStaffToken(
                staff, SecurityTokenType.STAFF_ACCOUNT_ACTIVATION);
        MailService.SendResult sendResult = mailService.sendStaffAccountActivationEmail(
                email, staff.getFullName(), staff.getUsername(), securityToken.getToken());

        if (!sendResult.success()) {
            throw new BadRequestException("Failed to send activation email: " + sendResult.message());
        }

        CreateStaffResponse.CreateStaffResponseBuilder builder = CreateStaffResponse.builder()
                .staff(mapper.toStaffResponse(staff))
                .message("Staff account created. Activation email sent to " + email)
                .email(email)
                .mailMode(sendResult.mode());

        if ("MOCK".equalsIgnoreCase(sendResult.mode())) {
            builder.devActionUrl(sendResult.actionUrl());
        }

        return builder.build();
    }

    @Transactional
    public VerifyEmailTokenResponse verifyStaffEmail(String tokenValue) {
        SecurityToken securityToken = securityTokenService.requireValidToken(
                tokenValue, SecurityTokenType.STAFF_ACCOUNT_ACTIVATION);

        Staff staff = securityToken.getStaff();
        if (staff == null || staff.isDeleted()) {
            throw new BadRequestException("Invalid or expired security token");
        }
        if (staff.getStatus() == StaffStatus.ACTIVE) {
            securityTokenService.markUsed(securityToken);
            return VerifyEmailTokenResponse.builder()
                    .success(true)
                    .message("Account is already active. You can sign in now.")
                    .build();
        }
        if (staff.getStatus() == StaffStatus.INACTIVE) {
            throw new BadRequestException("Account is inactive. Contact administrator.");
        }

        staff.setStatus(StaffStatus.ACTIVE);
        staffRepository.save(staff);
        securityTokenService.markUsed(securityToken);

        return VerifyEmailTokenResponse.builder()
                .success(true)
                .message("Staff account activated successfully. You can sign in now.")
                .build();
    }

    @Transactional
    public StaffForgotPasswordResponse requestPasswordResetByEmail(String email) {
        String normalized = MailService.normalizeEmail(email);
        MailService.SendResult sendResult = null;

        Optional<Staff> staffOpt = staffRepository.findByLoginId(normalized);
        if (staffOpt.isPresent() && staffOpt.get().getStatus() == StaffStatus.ACTIVE) {
            Staff staff = staffOpt.get();
            SecurityToken securityToken = securityTokenService.createStaffToken(
                    staff, SecurityTokenType.STAFF_PASSWORD_RESET);
            sendResult = mailService.sendStaffPasswordResetEmail(
                    staff.getEmail(), staff.getFullName(), securityToken.getToken());
        }

        StaffForgotPasswordResponse.StaffForgotPasswordResponseBuilder builder = StaffForgotPasswordResponse.builder()
                .message("If the email exists and the account is active, a password reset link has been sent.")
                .mailMode(mailService.getMailMode());

        if (sendResult != null && "MOCK".equalsIgnoreCase(sendResult.mode())) {
            builder.devActionUrl(sendResult.actionUrl());
        }

        return builder.build();
    }

    @Transactional
    public void resetPasswordByToken(StaffResetPasswordTokenRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        SecurityToken securityToken = securityTokenService.requireValidToken(
                request.getToken(), SecurityTokenType.STAFF_PASSWORD_RESET);

        Staff staff = securityToken.getStaff();
        if (staff == null || staff.isDeleted()) {
            throw new BadRequestException("Invalid or expired security token");
        }
        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive. Contact administrator.");
        }

        staff.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        staff.setRequirePasswordChange(false);
        staffRepository.save(staff);
        securityTokenService.markUsed(securityToken);
    }

    @Transactional
    public CreateStaffResponse resetPassword(Long id) {
        Staff staff = requireActiveStaff(id);

        String tempPassword = StaffConstants.RESET_PASSWORD;
        staff.setPasswordHash(passwordEncoder.encode(tempPassword));
        staff.setRequirePasswordChange(true);
        staff = staffRepository.save(staff);

        notificationService.sendPasswordResetEmail(staff.getEmail(), staff.getUsername(), tempPassword);

        return CreateStaffResponse.builder()
                .staff(mapper.toStaffResponse(staff))
                .temporaryPassword(tempPassword)
                .message("Password reset. Temporary password sent to " + staff.getEmail())
                .email(staff.getEmail())
                .build();
    }

    @Transactional
    public StaffResponse updateStatus(Long id, UpdateStaffStatusRequest request) {
        StaffStatus status = request.getStatus();
        if (status == StaffStatus.PENDING_ACTIVATION) {
            throw new BadRequestException("Cannot manually set PENDING_ACTIVATION status");
        }
        Staff staff = requireActiveStaff(id);
        staff.setStatus(status);
        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse assignRoles(Long staffId, AssignRolesRequest request) {
        Staff staff = requireActiveStaff(staffId);
        Set<Role> roles = resolveRoles(request.getRoleIds());
        validateRoleAssignment(roles);
        staff.setRoles(roles);
        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public void changePassword(Long staffId, ChangePasswordRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        staff.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        staff.setRequirePasswordChange(false);
        staffRepository.save(staff);
    }

    @Transactional
    public DeleteStaffResponse deleteStaff(Long id, DeleteStaffRequest request) {
        boolean hardDelete = request != null && Boolean.TRUE.equals(request.getHardDelete());

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (!hardDelete && staff.isDeleted()) {
            throw new BadRequestException("Staff is already deleted");
        }

        UserPrincipal principal = getCurrentPrincipal();
        if (principal != null
                && principal.getUserType() == UserPrincipal.UserType.STAFF
                && principal.getId().equals(id)) {
            throw new BadRequestException("You cannot delete your own account");
        }

        if (!staff.isDeleted() && hasRole(staff, "ROLE_ADMIN")
                && staffRepository.countActiveByRoleName("ROLE_ADMIN") <= 1) {
            throw new BadRequestException("Cannot delete the last admin account");
        }

        if (hardDelete) {
            securityTokenRepository.deleteByStaff(staff);
            staffRepository.delete(staff);
            return DeleteStaffResponse.builder()
                    .staffId(id)
                    .deletionType("HARD")
                    .message("Staff permanently deleted")
                    .build();
        }

        if (!staff.isDeleted()) {
            String suffix = DELETE_SUFFIX_MARKER + UUID.randomUUID().toString().substring(0, 8);
            staff.setUsername(truncateWithSuffix(staff.getUsername(), suffix, 50));
            staff.setEmail(truncateWithSuffix(MailService.normalizeEmail(staff.getEmail()), suffix, 100));
            if (staff.getPhoneNumber() != null && !staff.getPhoneNumber().isBlank()) {
                staff.setPhoneNumber(truncateWithSuffix(staff.getPhoneNumber(), suffix, 15));
            }
            staff.setStatus(StaffStatus.INACTIVE);
            staff.setWorkStatus(StaffWorkStatus.OFF);
            staff.setDeletedAt(LocalDateTime.now());
            staffRepository.save(staff);
        }

        return DeleteStaffResponse.builder()
                .staffId(id)
                .deletionType("SOFT")
                .message("Staff soft deleted successfully")
                .build();
    }

    @Transactional
    public StaffResponse restoreStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (!staff.isDeleted()) {
            throw new BadRequestException("Staff is not deleted");
        }

        String restoredUsername = stripDeleteSuffix(staff.getUsername());
        String restoredEmail = stripDeleteSuffix(staff.getEmail());
        String restoredPhone = staff.getPhoneNumber() != null
                ? stripDeleteSuffix(staff.getPhoneNumber())
                : null;

        validateUniqueCredentials(restoredUsername, MailService.normalizeEmail(restoredEmail), restoredPhone, id);

        staff.setUsername(restoredUsername);
        staff.setEmail(MailService.normalizeEmail(restoredEmail));
        staff.setPhoneNumber(restoredPhone);
        staff.setDeletedAt(null);
        staff.setStatus(StaffStatus.INACTIVE);

        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    private Set<Role> resolveRoles(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BadRequestException("One or more roles not found");
        }
        return new HashSet<>(roles);
    }

    private void validateUniqueCredentials(String username, String email, String phone, Long excludeStaffId) {
        staffRepository.findByUsername(username).ifPresent(existing -> {
            if (excludeStaffId == null || !existing.getStaffId().equals(excludeStaffId)) {
                throw new BadRequestException("Username already exists");
            }
        });
        if (customerRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }

        staffRepository.findByLoginId(email).ifPresent(existing -> {
            if (excludeStaffId == null || !existing.getStaffId().equals(excludeStaffId)) {
                throw new BadRequestException("Email already exists");
            }
        });
        if (customerRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        if (phone != null && !phone.isBlank()) {
            staffRepository.findByPhoneNumber(phone).ifPresent(existing -> {
                if (excludeStaffId == null || !existing.getStaffId().equals(excludeStaffId)) {
                    throw new BadRequestException("Phone number already exists");
                }
            });
            if (customerRepository.existsByPhoneNumber(phone)) {
                throw new BadRequestException("Phone number already registered");
            }
        }
    }

    private void validateRoleAssignment(Set<Role> roles) {
        if (!currentUserHasRole("ROLE_ADMIN")) {
            for (Role role : roles) {
                String roleName = role.getRoleName();
                if ("ROLE_ADMIN".equals(roleName) || "ROLE_MANAGER".equals(roleName)) {
                    throw new BadRequestException("Only administrators can assign role: " + roleName);
                }
            }
        }
    }

    private boolean currentUserHasRole(String roleName) {
        UserPrincipal principal = getCurrentPrincipal();
        return principal != null && principal.hasRole(roleName);
    }

    private UserPrincipal getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private Staff requireActiveStaff(Long id) {
        return staffRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\s+", "");
        if (digits.startsWith("+84")) {
            return "0" + digits.substring(3);
        }
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    private static boolean hasRole(Staff staff, String roleName) {
        return staff.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    private static String truncateWithSuffix(String value, String suffix, int maxLength) {
        if (value == null) {
            return suffix.length() > maxLength ? suffix.substring(0, maxLength) : suffix;
        }
        String combined = value + suffix;
        return combined.length() > maxLength ? combined.substring(0, maxLength) : combined;
    }

    private static String stripDeleteSuffix(String value) {
        if (value == null) {
            return null;
        }
        int markerIndex = value.indexOf(DELETE_SUFFIX_MARKER);
        return markerIndex >= 0 ? value.substring(0, markerIndex) : value;
    }
}
