package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.NotificationService;
import com.autowashpro.autowashpro_be.modules.identity.StaffConstants;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.RoleRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper mapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> listStaff(String status, String keyword, int page, int size) {
        StaffStatus staffStatus = status != null && !status.isBlank()
                ? StaffStatus.valueOf(status.toUpperCase())
                : null;

        Page<Staff> result = staffRepository.search(
                staffStatus,
                keyword,
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
        Staff staff = staffRepository.findById(id)
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
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

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
            staff.setRoles(resolveRoles(request.getRoleIds()));
        }

        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse updateWorkStatus(Long id, StaffWorkStatus workStatus) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        staff.setWorkStatus(workStatus);
        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public CreateStaffResponse provisionStaff(CreateStaffRequest request) {
        if (staffRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String phone = normalizePhone(request.getPhoneNumber());
        if (phone != null && !phone.isBlank() && staffRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("Phone number already exists");
        }

        String tempPassword = StaffConstants.TEMP_PASSWORD;

        Staff staff = Staff.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(phone)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .fullName(request.getFullName())
                .requirePasswordChange(true)
                .status(StaffStatus.ACTIVE)
                .workStatus(StaffWorkStatus.IDLE)
                .performanceKpi(0.0)
                .totalJobsCompleted(0)
                .serviceRating(5.0)
                .roles(resolveRoles(request.getRoleIds()))
                .build();

        staff = staffRepository.save(staff);
        notificationService.sendStaffWelcomeEmail(staff.getEmail(), staff.getUsername(), tempPassword);

        return CreateStaffResponse.builder()
                .staff(mapper.toStaffResponse(staff))
                .temporaryPassword(tempPassword)
                .message("Staff account created. Temporary password sent to " + staff.getEmail())
                .build();
    }

    @Transactional
    public CreateStaffResponse resetPassword(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        String tempPassword = StaffConstants.RESET_PASSWORD;
        staff.setPasswordHash(passwordEncoder.encode(tempPassword));
        staff.setRequirePasswordChange(true);
        staff = staffRepository.save(staff);

        notificationService.sendPasswordResetEmail(staff.getEmail(), staff.getUsername(), tempPassword);

        return CreateStaffResponse.builder()
                .staff(mapper.toStaffResponse(staff))
                .temporaryPassword(tempPassword)
                .message("Password reset. Temporary password sent to " + staff.getEmail())
                .build();
    }

    @Transactional
    public StaffResponse updateStatus(Long id, StaffStatus status) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        staff.setStatus(status);
        return mapper.toStaffResponse(staffRepository.save(staff));
    }

    @Transactional
    public StaffResponse assignRoles(Long staffId, AssignRolesRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        staff.setRoles(resolveRoles(request.getRoleIds()));
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

    private Set<Role> resolveRoles(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BadRequestException("One or more roles not found");
        }
        return new HashSet<>(roles);
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
}
