package com.autowashpro.autowashpro_be.security;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
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
                .orElseGet(() -> customerRepository.findByPhoneNumber(username)
                        .map(this::toCustomerPrincipal)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)));
    }

    public UserPrincipal loadByUsernameAndType(String username, UserPrincipal.UserType userType) {
        if (userType == UserPrincipal.UserType.CUSTOMER) {
            return customerRepository.findByPhoneNumber(username)
                    .map(this::toCustomerPrincipal)
                    .orElse(null);
        }
        return staffRepository.findByUsername(username)
                .map(this::toStaffPrincipal)
                .orElse(null);
    }

    public UserPrincipal toStaffPrincipal(Staff staff) {
        Set<String> permissions = new HashSet<>();
        for (Role role : staff.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getPermissionCode());
            }
        }
        return new UserPrincipal(
                staff.getStaffId(),
                staff.getUsername(),
                staff.getPasswordHash(),
                UserPrincipal.UserType.STAFF,
                staff.getStatus() == StaffStatus.ACTIVE,
                permissions
        );
    }

    public UserPrincipal toCustomerPrincipal(Customer customer) {
        return new UserPrincipal(
                customer.getCustomerId(),
                customer.getPhoneNumber(),
                customer.getPasswordHash(),
                UserPrincipal.UserType.CUSTOMER,
                true,
                Set.of("ROLE_CUSTOMER")
        );
    }
}
