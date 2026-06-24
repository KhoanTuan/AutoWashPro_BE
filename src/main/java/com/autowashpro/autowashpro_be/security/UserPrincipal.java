package com.autowashpro.autowashpro_be.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    public enum UserType {
        STAFF,
        CUSTOMER
    }

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long id;
    private final String username;
    private final String password;
    private final UserType userType;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String password, UserType userType,
                         boolean active, Set<String> authorityCodes) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.userType = userType;
        this.active = active;
        this.authorities = authorityCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    /** Permission codes only (excludes ROLE_* authorities). */
    public List<String> getPermissionCodes() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(code -> !code.startsWith(ROLE_PREFIX))
                .sorted()
                .toList();
    }

    /** Role names for hasRole() / business rules (ROLE_ADMIN, ROLE_MANAGER, ...). */
    public List<String> getRoleCodes() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(code -> code.startsWith(ROLE_PREFIX))
                .sorted()
                .toList();
    }

    public boolean hasRole(String roleName) {
        return getRoleCodes().contains(roleName);
    }

    public boolean hasPermission(String permissionCode) {
        return getPermissionCodes().contains(permissionCode);
    }
}
