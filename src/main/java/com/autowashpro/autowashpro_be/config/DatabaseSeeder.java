package com.autowashpro.autowashpro_be.config;

import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.PermissionRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.RoleRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ALL_PERMISSIONS = List.of(
            "MANAGE_STAFF", "MANAGE_ROLE", "ASSIGN_ROLE", "CONFIG_RBAC_MATRIX",
            "CONFIG_LOYALTY_TIER", "VIEW_CUSTOMER_PROFILE", "MANAGE_CUSTOMER_STATUS", "OVERRIDE_LOYALTY_DATA",
            "CONFIG_SLOT_CAPACITY", "VIEW_SLOT_AVAILABILITY", "FORCE_RELEASE_SLOT",
            "MANAGE_SERVICE_CATALOG", "CONFIG_DYNAMIC_PRICING", "MONITOR_REALTIME_QUEUE", "VIEW_OPERATIONAL_KPI",
            "VIEW_FINANCIAL_LEDGER", "AUDIT_SHIFT_CLOSURE", "MANAGE_MARKETING_PROMOTION",
            "VIEW_AI_STRATEGY_REPORT", "MONITOR_AI_LOYALTY_AUTOMATION", "VIEW_FEEDBACK_SENTIMENT",
            "VIEW_SECURITY_AUDIT_LOG",
            "PROCESS_CHECKIN", "CREATE_WALKIN_ORDER", "CLOSE_SHIFT",
            "VIEW_TECH_QUEUE", "UPDATE_TASK_CHECKLIST"
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedLoyaltyTiers();
        if (permissionRepository.count() > 0) {
            log.info("Database already seeded.");
            return;
        }

        log.info("Seeding permissions, roles, and default admin...");

        Map<String, Permission> permissionMap = new LinkedHashMap<>();
        for (String code : ALL_PERMISSIONS) {
            permissionMap.put(code, permissionRepository.save(
                    Permission.builder().permissionCode(code).build()
            ));
        }

        Role adminRole = createRole("ROLE_ADMIN", "System administrator", permissionMap.values());
        Role cashierRole = createRole("ROLE_CASHIER", "Front desk cashier", Set.of(
                permissionMap.get("PROCESS_CHECKIN"),
                permissionMap.get("CREATE_WALKIN_ORDER"),
                permissionMap.get("CLOSE_SHIFT"),
                permissionMap.get("VIEW_CUSTOMER_PROFILE"),
                permissionMap.get("MONITOR_REALTIME_QUEUE")
        ));
        Role technicianRole = createRole("ROLE_TECHNICIAN", "Back bay technician", Set.of(
                permissionMap.get("VIEW_TECH_QUEUE"),
                permissionMap.get("UPDATE_TASK_CHECKLIST"),
                permissionMap.get("MONITOR_REALTIME_QUEUE")
        ));

        Staff admin = Staff.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .status(StaffStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        staffRepository.save(admin);

        Staff cashier = Staff.builder()
                .username("cashier")
                .passwordHash(passwordEncoder.encode("Cashier@123"))
                .fullName("Demo Cashier")
                .status(StaffStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(cashierRole)))
                .build();
        staffRepository.save(cashier);

        Staff technician = Staff.builder()
                .username("technician")
                .passwordHash(passwordEncoder.encode("Tech@123"))
                .fullName("Demo Technician")
                .status(StaffStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(technicianRole)))
                .build();
        staffRepository.save(technician);

        log.info("Seed complete. Admin: admin / Admin@123");
    }

    private void seedLoyaltyTiers() {
        if (loyaltyTierRepository.count() > 0) return;
        loyaltyTierRepository.saveAll(List.of(
                tier("MEMBER", "0", "1.00", 7),
                tier("SILVER", "1000000", "1.20", 10),
                tier("GOLD", "5000000", "1.50", 12),
                tier("PLATINUM", "10000000", "2.00", 14)
        ));
    }

    private LoyaltyTier tier(String name, String minSpend, String multiplier, int windowDays) {
        return LoyaltyTier.builder()
                .tierName(name)
                .minSpend(new BigDecimal(minSpend))
                .tierMultiplier(new BigDecimal(multiplier))
                .bookingWindowDays(windowDays)
                .build();
    }

    private Role createRole(String name, String description, Collection<Permission> permissions) {
        Role role = Role.builder()
                .roleName(name)
                .description(description)
                .permissions(new HashSet<>(permissions))
                .build();
        return roleRepository.save(role);
    }
}
