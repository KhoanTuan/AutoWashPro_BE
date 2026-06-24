package com.autowashpro.autowashpro_be.security;

import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return staffRepository.findByUsername(username)
                .map(this::toStaffPrincipal)
                .orElseGet(() -> findCustomerPrincipal(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)));
    }

    public UserPrincipal loadByUsernameAndType(String username, UserPrincipal.UserType userType) {
        if (userType == UserPrincipal.UserType.CUSTOMER) {
            return findCustomerPrincipal(username).orElse(null);
        }
        return staffRepository.findByUsername(username)
                .map(this::toStaffPrincipal)
                .orElse(null);
    }

    private Optional<UserPrincipal> findCustomerPrincipal(String loginId) {
        if (loginId.contains("@")) {
            Optional<UserPrincipal> byEmail = customerRepository.findByEmail(MailService.normalizeEmail(loginId))
                    .map(this::toCustomerPrincipal);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }

        String phone = normalizePhone(loginId);
        if (phone.matches("^0\\d{9,10}$")) {
            Optional<UserPrincipal> byPhone = customerRepository.findByPhoneNumber(phone)
                    .map(this::toCustomerPrincipal);
            if (byPhone.isPresent()) {
                return byPhone;
            }
        }

        return customerRepository.findByUsername(loginId.trim()).map(this::toCustomerPrincipal);
    }

    private static String normalizePhone(String input) {
        String digits = input.replaceAll("\\s+", "");
        if (digits.startsWith("+84")) {
            return "0" + digits.substring(3);
        }
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    public UserPrincipal toStaffPrincipal(Staff staff) {
        Set<String> authorityCodes = new HashSet<>();
        for (Role role : staff.getRoles()) {
            if (role.getRoleName() != null) {
                authorityCodes.add(role.getRoleName());
            }
            for (Permission permission : role.getPermissions()) {
                if (permission.getPermissionCode() != null
                        && Boolean.TRUE.equals(permission.getEnabled())) {
                    authorityCodes.add(permission.getPermissionCode());
                }
            }
        }
        return new UserPrincipal(
                staff.getStaffId(),
                staff.getUsername(),
                staff.getPasswordHash(),
                UserPrincipal.UserType.STAFF,
                staff.getStatus() == StaffStatus.ACTIVE,
                authorityCodes
        );
    }

    public UserPrincipal toCustomerPrincipal(Customer customer) {
        String principalName = customer.getPhoneNumber() != null && !customer.getPhoneNumber().isBlank()
                ? customer.getPhoneNumber()
                : (customer.getEmail() != null && !customer.getEmail().isBlank()
                ? customer.getEmail()
                : customer.getUsername());
        return new UserPrincipal(
                customer.getCustomerId(),
                principalName,
                customer.getPasswordHash(),
                UserPrincipal.UserType.CUSTOMER,
                customer.getStatus() == CustomerStatus.ACTIVE,
                Set.of("ROLE_CUSTOMER")
        );
    }
}
