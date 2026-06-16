package com.autowashpro.autowashpro_be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generateStaffToken(Long staffId, String username, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", UserPrincipal.UserType.STAFF.name());
        claims.put("staffId", staffId);
        claims.put("permissions", permissions);
        return buildToken(claims, username);
    }

    public String generateCustomerToken(Long customerId, String phoneNumber) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", UserPrincipal.UserType.CUSTOMER.name());
        claims.put("customerId", customerId);
        return buildToken(claims, phoneNumber);
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UserPrincipal.UserType extractUserType(String token) {
        String type = extractClaim(token, claims -> claims.get("userType", String.class));
        return UserPrincipal.UserType.valueOf(type);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> claims.get("permissions", List.class));
    }

    public Long extractStaffId(String token) {
        return extractClaim(token, claims -> claims.get("staffId", Long.class));
    }

    public Long extractCustomerId(String token) {
        return extractClaim(token, claims -> claims.get("customerId", Long.class));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
