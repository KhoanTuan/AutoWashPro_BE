package com.autowashpro.autowashpro_be.config;

import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceVariantRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.SlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.WashServiceRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerAuthProvider;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final SlotRepository slotRepository;
    private final WashServiceRepository washServiceRepository;
    private final ServiceVariantRepository serviceVariantRepository;
    private final PasswordEncoder passwordEncoder;

    private Map<String, Permission> ensurePermissions() {
        Map<String, Permission> map = new LinkedHashMap<>();
        for (PermissionCatalog.Definition def : PermissionCatalog.ALL) {
            Permission permission = permissionRepository.findByPermissionCode(def.getCode())
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .permissionCode(def.getCode())
                            .build()));
            permission.setDescription(def.getLabel());
            permission.setModuleGroup(def.getModuleGroup());
            permission.setPhase(def.getPhase());
            permission.setEnabled(def.isEnabled());
            map.put(def.getCode(), permissionRepository.save(permission));
        }
        return map;
    }

    private Set<Permission> pick(Map<String, Permission> map, String... codes) {
        Set<Permission> result = new LinkedHashSet<>();
        for (String code : codes) {
            if (map.get(code) != null) {
                result.add(map.get(code));
            }
        }
        return result;
    }

    private Map<String, Role> ensureRoles(Map<String, Permission> permissionMap) {
        Map<String, Role> roles = new HashMap<>();

        Collection<Permission> adminPerms = permissionMap.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                .toList();

        roles.put("ROLE_ADMIN", ensureRole("ROLE_ADMIN", "System Administrator", adminPerms));

        roles.put("ROLE_MANAGER", ensureRole("ROLE_MANAGER", "Station Manager", pick(permissionMap,
                PermissionCatalog.READ_STAFF, PermissionCatalog.CREATE_UPDATE_STAFF, "ASSIGN_ROLE",
                "VIEW_CUSTOMER_PROFILE", "MANAGE_CUSTOMER_STATUS",
                "CREATE_WALK_IN_BOOKING", "CASHIER_CHECKIN", "VIEW_SLOT_AVAILABILITY",
                "MONITOR_REALTIME_QUEUE", "VIEW_TECH_QUEUE"
        )));

        roles.put("ROLE_TECHNICIAN", ensureRole("ROLE_TECHNICIAN", "Bay Technician", pick(permissionMap,
                "VIEW_TECH_QUEUE", "TASK_CHECKLIST", "MONITOR_REALTIME_QUEUE"
        )));

        roles.put("ROLE_CASHIER", ensureRole("ROLE_CASHIER", "Front Desk Cashier", pick(permissionMap,
                "VIEW_CUSTOMER_PROFILE", "CREATE_WALK_IN_BOOKING", "CASHIER_CHECKIN", "VIEW_SLOT_AVAILABILITY"
        )));

        return roles;
    }
    private static final List<DemoStaffSeed> DEMO_STAFF = List.of(
            new DemoStaffSeed("admin", "admin@autowashpro.com", "0901000001", "Nguyen Van Admin",
                    "Admin@123", "ROLE_ADMIN", StaffStatus.ACTIVE, StaffWorkStatus.IDLE,
                    false, 99.0, 210, 5.0),
            new DemoStaffSeed("manager", "manager@autowashpro.com", "0901000002", "Tran Thi Manager",
                    "Manager@123", "ROLE_MANAGER", StaffStatus.ACTIVE, StaffWorkStatus.IDLE,
                    false, 96.5, 185, 4.9),
            new DemoStaffSeed("tech01", "tech01@autowashpro.com", "0901000003", "Le Van An",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.IDLE,
                    false, 98.0, 142, 4.9),
            new DemoStaffSeed("tech02", "tech02@autowashpro.com", "0901000004", "Pham Thi Binh",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.BUSY,
                    false, 91.0, 128, 4.8),
            new DemoStaffSeed("tech03", "tech03@autowashpro.com", "0901000005", "Hoang Minh Cuong",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.ON_BREAK,
                    false, 88.0, 89, 4.7),
            new DemoStaffSeed("tech04", "tech04@autowashpro.com", "0901000006", "Vo Thi Dung",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.OFF,
                    false, 92.0, 115, 4.8),
            new DemoStaffSeed("tech05", "tech05@autowashpro.com", "0901000007", "Dang Quoc Em",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.IDLE,
                    false, 72.0, 45, 4.2),
            new DemoStaffSeed("tech06", "tech06@autowashpro.com", "0901000008", "Bui Van Phuc",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.BUSY,
                    true, 85.0, 67, 4.5),
            new DemoStaffSeed("tech07", "tech07@autowashpro.com", "0901000009", "Ngo Thi Giang",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.INACTIVE, StaffWorkStatus.OFF,
                    false, 80.0, 52, 4.0),
            new DemoStaffSeed("tech08", "tech08@autowashpro.com", "0901000010", "Do Van Hieu",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.IDLE,
                    false, 97.5, 156, 5.0),
            new DemoStaffSeed("tech09", "tech09@autowashpro.com", "0901000011", "Ly Thi Khanh",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.ON_BREAK,
                    false, 76.0, 38, 3.8),
            new DemoStaffSeed("tech10", "tech10@autowashpro.com", "0901000012", "Mai Van Long",
                    "Tech@123", "ROLE_TECHNICIAN", StaffStatus.ACTIVE, StaffWorkStatus.OFF,
                    false, 0.0, 0, 5.0)
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedLoyaltyTiers();
        Map<String, Permission> permissionMap = ensurePermissions();
        Map<String, Role> rolesByName = ensureRoles(permissionMap);
        seedDemoStaff(rolesByName);
        patchExistingStaffDefaults();
        seedCatalog();
        seedDemoCustomers();
        log.info("Demo staff ready — admin/Admin@123, manager/Manager@123, tech01-10/Tech@123");
        log.info("Demo customers ready — password Customer@123");
    }

    private Role ensureRole(String roleName, String description, Collection<Permission> permissions) {
        Role role = roleRepository.findByRoleName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .description(description)
                        .permissions(new HashSet<>())
                        .build())
        );
        role.setDescription(description);
        role.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(role);
    }

    private void seedDemoStaff(Map<String, Role> rolesByName) {
        for (DemoStaffSeed seed : DEMO_STAFF) {
            Role role = rolesByName.get(seed.roleName());
            if (role == null) {
                log.warn("Role {} not found — skip staff {}", seed.roleName(), seed.username());
                continue;
            }

            staffRepository.findByUsername(seed.username()).ifPresentOrElse(
                    existing -> updateDemoStaff(existing, seed, role),
                    () -> staffRepository.save(buildDemoStaff(seed, role))
            );
        }
    }

    private Staff buildDemoStaff(DemoStaffSeed seed, Role role) {
        return Staff.builder()
                .username(seed.username())
                .email(seed.email())
                .phoneNumber(seed.phone())
                .fullName(seed.fullName())
                .passwordHash(passwordEncoder.encode(seed.password()))
                .requirePasswordChange(seed.requirePasswordChange())
                .status(seed.accountStatus())
                .workStatus(seed.workStatus())
                .performanceKpi(seed.performanceKpi())
                .totalJobsCompleted(seed.totalJobs())
                .serviceRating(seed.serviceRating())
                .roles(new HashSet<>(Set.of(role)))
                .build();
    }

    private void updateDemoStaff(Staff staff, DemoStaffSeed seed, Role role) {
        staff.setEmail(seed.email());
        staff.setPhoneNumber(seed.phone());
        staff.setFullName(seed.fullName());
        staff.setRequirePasswordChange(seed.requirePasswordChange());
        staff.setStatus(seed.accountStatus());
        staff.setWorkStatus(seed.workStatus());
        staff.setPerformanceKpi(seed.performanceKpi());
        staff.setTotalJobsCompleted(seed.totalJobs());
        staff.setServiceRating(seed.serviceRating());
        staff.setRoles(new HashSet<>(Set.of(role)));
        staffRepository.save(staff);
    }

    private void patchExistingStaffDefaults() {
        for (Staff staff : staffRepository.findAll()) {
            if (staff.getEmail() == null || staff.getEmail().isBlank()) {
                staff.setEmail(staff.getUsername() + "@autowashpro.com");
            }
            if (staff.getRequirePasswordChange() == null) {
                staff.setRequirePasswordChange(false);
            }
            if (staff.getWorkStatus() == null) {
                staff.setWorkStatus(StaffWorkStatus.IDLE);
            }
            if (staff.getPerformanceKpi() == null) {
                staff.setPerformanceKpi(0.0);
            }
            if (staff.getTotalJobsCompleted() == null) {
                staff.setTotalJobsCompleted(0);
            }
            if (staff.getServiceRating() == null) {
                staff.setServiceRating(5.0);
            }
            staffRepository.save(staff);
        }
    }

    private void seedLoyaltyTiers() {
        if (loyaltyTierRepository.count() > 0) return;
        loyaltyTierRepository.saveAll(List.of(
                tier("REGULAR", "0", "1.00", 7),
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

    private void seedCatalog() {
        if (slotRepository.count() == 0) {
            slotRepository.saveAll(List.of(
                    slot(LocalTime.of(8, 0), LocalTime.of(9, 0), 4),
                    slot(LocalTime.of(9, 0), LocalTime.of(10, 0), 4),
                    slot(LocalTime.of(10, 0), LocalTime.of(11, 0), 4),
                    slot(LocalTime.of(11, 0), LocalTime.of(12, 0), 4),
                    slot(LocalTime.of(13, 0), LocalTime.of(14, 0), 4),
                    slot(LocalTime.of(14, 0), LocalTime.of(15, 0), 4),
                    slot(LocalTime.of(15, 0), LocalTime.of(16, 0), 4),
                    slot(LocalTime.of(16, 0), LocalTime.of(17, 0), 4)
            ));
        }

        if (washServiceRepository.count() == 0) {
            WashService basic = washServiceRepository.save(WashService.builder()
                    .serviceName("Basic Wash")
                    .basePrice(new BigDecimal("250000"))
                    .durationMinutes(30)
                    .build());
            WashService premium = washServiceRepository.save(WashService.builder()
                    .serviceName("Premium Wash")
                    .basePrice(new BigDecimal("450000"))
                    .durationMinutes(45)
                    .build());
            WashService detail = washServiceRepository.save(WashService.builder()
                    .serviceName("Full Detail")
                    .basePrice(new BigDecimal("890000"))
                    .durationMinutes(90)
                    .build());

            for (WashService service : List.of(basic, premium, detail)) {
                for (CarType carType : CarType.values()) {
                    BigDecimal multiplier = switch (carType) {
                        case SUV -> new BigDecimal("1.20");
                        case TRUCK -> new BigDecimal("1.40");
                        default -> BigDecimal.ONE;
                    };
                    serviceVariantRepository.save(ServiceVariant.builder()
                            .service(service)
                            .carType(carType)
                            .calculatedPrice(service.getBasePrice().multiply(multiplier))
                            .build());
                }
            }
        }
    }

    private Slot slot(LocalTime start, LocalTime end, int capacity) {
        return Slot.builder().startTime(start).endTime(end).maxCapacity(capacity).build();
    }

    private void seedDemoCustomers() {
        Map<String, LoyaltyTier> tiers = new HashMap<>();
        loyaltyTierRepository.findAll().forEach(t -> tiers.put(t.getTierName(), t));

        List<DemoCustomerSeed> seeds = List.of(
                new DemoCustomerSeed("Nguyen Van An", "0902000001", "an.nguyen@email.com", "51A-12345", CarType.SEDAN, "REGULAR", CustomerStatus.ACTIVE, 12, 2400000, 850),
                new DemoCustomerSeed("Tran Thi Binh", "0902000002", "binh.tran@email.com", "51B-67890", CarType.SUV, "SILVER", CustomerStatus.ACTIVE, 28, 5200000, 2100),
                new DemoCustomerSeed("Le Minh Cuong", "0902000003", "cuong.le@email.com", "30C-11223", CarType.SEDAN, "GOLD", CustomerStatus.ACTIVE, 45, 9800000, 4800),
                new DemoCustomerSeed("Pham Thi Dung", "0902000004", "dung.pham@email.com", "43D-44556", CarType.TRUCK, "PLATINUM", CustomerStatus.ACTIVE, 62, 15200000, 9200),
                new DemoCustomerSeed("Hoang Van Em", "0902000005", "em.hoang@email.com", "59E-77889", CarType.SEDAN, "REGULAR", CustomerStatus.ACTIVE, 5, 650000, 120),
                new DemoCustomerSeed("Vo Thi Phuong", "0902000006", "phuong.vo@email.com", "77F-99001", CarType.SUV, "SILVER", CustomerStatus.INACTIVE, 18, 3100000, 900),
                new DemoCustomerSeed("Dang Quoc Giang", "0902000007", "giang.dang@email.com", "92G-22334", CarType.SEDAN, "REGULAR", CustomerStatus.ACTIVE, 2, 180000, 40),
                new DemoCustomerSeed("Bui Thi Hoa", "0902000008", "hoa.bui@email.com", "61H-55667", CarType.SEDAN, "GOLD", CustomerStatus.ACTIVE, 35, 7200000, 3500)
        );

        for (DemoCustomerSeed seed : seeds) {
            customerRepository.findByPhoneNumber(seed.phone()).ifPresentOrElse(
                    existing -> updateDemoCustomer(existing, seed, tiers.get(seed.tierName())),
                    () -> createDemoCustomer(seed, tiers.get(seed.tierName()))
            );
        }
    }

    private void createDemoCustomer(DemoCustomerSeed seed, LoyaltyTier tier) {
        Customer customer = customerRepository.save(Customer.builder()
                .fullName(seed.fullName())
                .phoneNumber(seed.phone())
                .email(seed.email())
                .authProvider(CustomerAuthProvider.PHONE)
                .status(seed.status())
                .passwordHash(passwordEncoder.encode("Customer@123"))
                .tier(tier)
                .visitCount(seed.visits())
                .totalSpending(new BigDecimal(seed.spending()))
                .loyaltyPoints(seed.points())
                .lastCompletedBookingAt(seed.visits() > 0 ? LocalDateTime.now().minusDays(3) : null)
                .build());

        vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .licensePlate(seed.plate())
                .carType(seed.carType())
                .build());
    }

    private void updateDemoCustomer(Customer customer, DemoCustomerSeed seed, LoyaltyTier tier) {
        customer.setFullName(seed.fullName());
        customer.setEmail(seed.email());
        customer.setStatus(seed.status());
        customer.setTier(tier);
        customer.setVisitCount(seed.visits());
        customer.setTotalSpending(new BigDecimal(seed.spending()));
        customer.setLoyaltyPoints(seed.points());
        customerRepository.save(customer);

        vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(customer.getCustomerId())
                .ifPresentOrElse(
                        v -> {
                            v.setLicensePlate(seed.plate());
                            v.setCarType(seed.carType());
                            vehicleRepository.save(v);
                        },
                        () -> vehicleRepository.save(Vehicle.builder()
                                .customer(customer)
                                .licensePlate(seed.plate())
                                .carType(seed.carType())
                                .build())
                );
    }

    private record DemoCustomerSeed(
            String fullName,
            String phone,
            String email,
            String plate,
            CarType carType,
            String tierName,
            CustomerStatus status,
            int visits,
            long spending,
            int points
    ) {}

    private record DemoStaffSeed(
            String username,
            String email,
            String phone,
            String fullName,
            String password,
            String roleName,
            StaffStatus accountStatus,
            StaffWorkStatus workStatus,
            boolean requirePasswordChange,
            double performanceKpi,
            int totalJobs,
            double serviceRating
    ) {}
}
