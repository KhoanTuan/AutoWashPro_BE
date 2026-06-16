package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.modules.identity.dto.JwtResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.LoginRequest;
import com.autowashpro.autowashpro_be.modules.identity.service.AuthService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<JwtResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.STAFF) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(authService.getCurrentStaffProfile(principal));
    }
}
