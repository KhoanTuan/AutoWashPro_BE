package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.config.SecurityTokenProperties;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityToken;
import com.autowashpro.autowashpro_be.modules.customer.entity.SecurityTokenType;
import com.autowashpro.autowashpro_be.modules.customer.repository.SecurityTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SecurityTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecurityTokenRepository securityTokenRepository;
    private final SecurityTokenProperties securityTokenProperties;

    @Transactional
    public SecurityToken createToken(Customer customer, SecurityTokenType type) {
        securityTokenRepository.invalidateActiveTokens(customer, type);

        int ttlMinutes = type == SecurityTokenType.EMAIL_VERIFICATION
                ? securityTokenProperties.getEmailVerificationMinutes()
                : securityTokenProperties.getPasswordResetMinutes();

        SecurityToken token = SecurityToken.builder()
                .customer(customer)
                .tokenType(type)
                .token(generateSecureToken())
                .expiresAt(Instant.now().plusSeconds(ttlMinutes * 60L))
                .build();

        return securityTokenRepository.save(token);
    }

    public SecurityToken requireValidToken(String rawToken, SecurityTokenType expectedType) {
        SecurityToken token = securityTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired security token"));

        if (token.getTokenType() != expectedType) {
            throw new BadRequestException("Invalid token type for this action");
        }
        if (token.isUsed()) {
            throw new BadRequestException("Security token has already been used");
        }
        if (token.isExpired()) {
            throw new BadRequestException("Security token has expired");
        }
        return token;
    }

    @Transactional
    public void markUsed(SecurityToken token) {
        token.setUsed(true);
        securityTokenRepository.save(token);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
