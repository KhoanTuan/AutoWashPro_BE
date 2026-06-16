package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAuthService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register")
    public ResponseEntity<CustomerAuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAuthService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(customerAuthService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerAuthService.getProfile(principal));
    }
}
