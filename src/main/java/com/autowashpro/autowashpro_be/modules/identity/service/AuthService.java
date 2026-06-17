package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final StaffRepository staffRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Staff staff = staffRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Account is inactive. Contact administrator.");
        }

        return buildJwtResponse(staff);
    }

    @Transactional
    public JwtResponse changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        staffService.changePassword(principal.getId(), request);

        Staff staff = staffRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Staff not found"));

        return buildJwtResponse(staff);
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
                .username(staff.getUsername())
                .fullName(staff.getFullName())
                .roles(roles)
                .permissions(permissions)
                .forceChangePassword(forceChange)
                .build();
    }
}
