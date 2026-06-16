package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
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

    @Transactional
    public CustomerAuthResponse register(CustomerRegisterRequest request) {
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        LoyaltyTier memberTier = loyaltyTierRepository.findByTierName("MEMBER")
                .orElseThrow(() -> new BadRequestException("Default tier not configured"));

        Customer customer = Customer.builder()
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .tier(memberTier)
                .build();

        customer = customerRepository.save(customer);
        return buildAuthResponse(customer);
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
