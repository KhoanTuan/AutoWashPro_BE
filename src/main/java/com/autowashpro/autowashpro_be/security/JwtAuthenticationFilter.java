package com.autowashpro.autowashpro_be.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtTokenProvider.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserPrincipal.UserType userType = jwtTokenProvider.extractUserType(token);
            UserPrincipal principal = userDetailsService.loadByUsernameAndType(username, userType);

            if (principal != null && principal.isEnabled()) {
                if (userType == UserPrincipal.UserType.STAFF) {
                    Set<String> merged = new HashSet<>();
                    merged.addAll(principal.getRoleCodes());
                    merged.addAll(principal.getPermissionCodes());
                    List<String> tokenRoles = jwtTokenProvider.extractRoles(token);
                    List<String> tokenPermissions = jwtTokenProvider.extractPermissions(token);
                    if (tokenRoles != null) {
                        merged.addAll(tokenRoles);
                    }
                    if (tokenPermissions != null) {
                        merged.addAll(tokenPermissions);
                    }
                    principal = new UserPrincipal(
                            principal.getId(),
                            principal.getUsername(),
                            principal.getPassword(),
                            principal.getUserType(),
                            principal.isEnabled(),
                            merged
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
