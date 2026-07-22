package com.autowashpro.autowashpro_be.config;


import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingItemRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceCatalogRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.notification.entity.*;
import com.autowashpro.autowashpro_be.modules.notification.repository.NotificationRepository;
import com.autowashpro.autowashpro_be.modules.marketing.entity.*;
import com.autowashpro.autowashpro_be.modules.marketing.repository.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerAuthProvider;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
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
import java.time.LocalDate;
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
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final NotificationRepository notificationRepository;
    private final PromotionRepository promotionRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final CustomerFeedbackRepository customerFeedbackRepository;
    private final PointTransactionRepository pointTransactionRepository;

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
                PermissionCatalog.READ_STAFF, PermissionCatalog.CREATE_UPDATE_STAFF, PermissionCatalog.ASSIGN_ROLE,
                PermissionCatalog.VIEW_CUSTOMER_PROFILE, PermissionCatalog.MANAGE_CUSTOMER_STATUS, PermissionCatalog.MANAGE_LOYALTY_CONFIG,
                PermissionCatalog.CREATE_WALK_IN_BOOKING, PermissionCatalog.CASHIER_CHECKIN, PermissionCatalog.CANCEL_BOOKING,
                PermissionCatalog.VIEW_SLOT_AVAILABILITY, PermissionCatalog.VIEW_STATION_QUEUE, PermissionCatalog.MANAGE_WASH_PROGRESS,
                PermissionCatalog.MONITOR_REALTIME_QUEUE, PermissionCatalog.VIEW_DASHBOARD_STATS,
                PermissionCatalog.MANAGE_SERVICE_CATALOG, PermissionCatalog.MANAGE_SLOT_CONFIG, PermissionCatalog.MANAGE_STATION_SETTINGS,
                PermissionCatalog.SEND_INCIDENT_ALERT
        )));

        roles.put("ROLE_CASHIER", ensureRole("ROLE_CASHIER", "Front Desk Cashier", pick(permissionMap,
                PermissionCatalog.VIEW_CUSTOMER_PROFILE, PermissionCatalog.CREATE_WALK_IN_BOOKING,
                PermissionCatalog.CASHIER_CHECKIN, PermissionCatalog.CANCEL_BOOKING, PermissionCatalog.VIEW_SLOT_AVAILABILITY,
                PermissionCatalog.VIEW_STATION_QUEUE, PermissionCatalog.MANAGE_WASH_PROGRESS, PermissionCatalog.MONITOR_REALTIME_QUEUE,
                PermissionCatalog.SEND_INCIDENT_ALERT
        )));

        return roles;
    }

    private static final List<DemoStaffSeed> DEMO_STAFF = List.of(
            new DemoStaffSeed("admin", "admin@autowashpro.com", "0901000001", "Nguyen Van Admin",
                    "Admin@123", "ROLE_ADMIN", StaffStatus.ACTIVE, false),
            new DemoStaffSeed("manager", "manager@autowashpro.com", "0901000002", "Tran Thi Manager",
                    "Manager@123", "ROLE_MANAGER", StaffStatus.ACTIVE, false),
            new DemoStaffSeed("cashier", "cashier@autowashpro.com", "0901000003", "Le Van Cashier",
                    "Cashier@123", "ROLE_CASHIER", StaffStatus.ACTIVE, false),
            new DemoStaffSeed("fired_staff", "fired@autowashpro.com", "0901000004", "Tran Fired Staff",
                    "Staff@123", "ROLE_CASHIER", StaffStatus.INACTIVE, false)
    );

    private void cleanupDatabase() {
        log.info("Cleaning up existing database tables for fresh seeding...");
        customerFeedbackRepository.deleteAllInBatch();
        customerPromotionRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        pointTransactionRepository.deleteAllInBatch();
        
        bookingItemRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        
        staffRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
        permissionRepository.deleteAllInBatch();
        
        timeSlotRepository.deleteAllInBatch();
        serviceCatalogRepository.deleteAllInBatch();
        loyaltyTierRepository.deleteAllInBatch();
        log.info("Database cleanup completed!");
    }

    @Override
    @Transactional
    public void run(String... args) {
        cleanupDatabase();
        seedLoyaltyTiers();
        Map<String, Permission> permissionMap = ensurePermissions();
        Map<String, Role> rolesByName = ensureRoles(permissionMap);
        seedDemoStaff(rolesByName);
        patchExistingStaffDefaults();
        seedDemoCustomers();
        seedDemoVehicles();
        seedServiceCatalog();
        seedTimeSlots();
        seedDemoBookings();
        seedPointTransactions();
        seedDemoNotifications();
        seedDemoPromotionsAndFeedbacks();
        log.info("Demo staff ready — admin/Admin@123, manager/Manager@123, cashier/Cashier@123, fired_staff/Staff@123 (Inactive)");
        log.info("Demo customers ready — password Customer@123");
        log.info("Demo service catalog, time slots, bookings, point transactions, notifications, promotions & feedbacks seeded successfully!");
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
                .roles(new HashSet<>(Set.of(role)))
                .build();
    }

    private void updateDemoStaff(Staff staff, DemoStaffSeed seed, Role role) {
        staff.setEmail(seed.email());
        staff.setPhoneNumber(seed.phone());
        staff.setFullName(seed.fullName());
        staff.setRequirePasswordChange(seed.requirePasswordChange());
        staff.setStatus(seed.accountStatus());
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
            staffRepository.save(staff);
        }
    }

    private void seedLoyaltyTiers() {
        if (loyaltyTierRepository.count() > 0) {
            return;
        }
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

    private void seedDemoCustomers() {
        Map<String, LoyaltyTier> tiers = new HashMap<>();
        loyaltyTierRepository.findAll().forEach(t -> {
            tiers.put(t.getTierName(), t);
            if ("MEMBER".equalsIgnoreCase(t.getTierName())) {
                tiers.put("REGULAR", t);
            }
        });

        List<DemoCustomerSeed> seeds = List.of(
                new DemoCustomerSeed("annguyen", "Nguyen Van An", "0902000001", "an.nguyen@email.com", "MEMBER", CustomerStatus.ACTIVE, 12, 2400000, 850, 3),
                new DemoCustomerSeed("binhtran", "Tran Thi Binh", "0902000002", "binh.tran@email.com", "SILVER", CustomerStatus.ACTIVE, 28, 5200000, 2100, 5),
                new DemoCustomerSeed("cuongle", "Le Minh Cuong", "0902000003", "cuong.le@email.com", "GOLD", CustomerStatus.ACTIVE, 45, 9800000, 4800, 2),
                new DemoCustomerSeed("dungpham", "Pham Thi Dung", "0902000004", "dung.pham@email.com", "PLATINUM", CustomerStatus.ACTIVE, 62, 15200000, 9200, 1),
                new DemoCustomerSeed("emhoang", "Hoang Van Em", "0902000005", "em.hoang@email.com", "MEMBER", CustomerStatus.ACTIVE, 5, 650000, 120, 15),
                new DemoCustomerSeed("phuongvo", "Vo Thi Phuong", "0902000006", "phuong.vo@email.com", "SILVER", CustomerStatus.INACTIVE, 18, 3100000, 900, 25),
                new DemoCustomerSeed("giangdang", "Dang Quoc Giang", "0902000007", "giang.dang@email.com", "MEMBER", CustomerStatus.ACTIVE, 2, 180000, 40, 45),
                new DemoCustomerSeed("hoabui", "Bui Thi Hoa", "0902000008", "hoa.bui@email.com", "GOLD", CustomerStatus.ACTIVE, 35, 7200000, 3500, 8),
                new DemoCustomerSeed("khanhnguyen", "Nguyen Van Khanh", "0902000009", "khanh.nguyen@email.com", "PLATINUM", CustomerStatus.ACTIVE, 50, 12000000, 6000, 35),
                new DemoCustomerSeed("lamtran", "Tran Van Lam", "0902000010", "lam.tran@email.com", "MEMBER", CustomerStatus.ACTIVE, 8, 1100000, 300, 50),
                new DemoCustomerSeed("maile", "Le Thi Mai", "0902000011", "mai.le@email.com", "SILVER", CustomerStatus.ACTIVE, 20, 4200000, 1800, 12),
                new DemoCustomerSeed("nampham", "Pham Hoang Nam", "0902000012", "nam.pham@email.com", "GOLD", CustomerStatus.ACTIVE, 40, 8500000, 4200, 60),
                new DemoCustomerSeed("oanhtran", "Tran Thi Oanh", "0902000013", "oanh.tran@email.com", "MEMBER", CustomerStatus.ACTIVE, 1, 50000, 10, 95),
                new DemoCustomerSeed("phucle", "Le Huu Phuc", "0902000014", "phuc.le@email.com", "SILVER", CustomerStatus.ACTIVE, 15, 2800000, 1100, 4),
                new DemoCustomerSeed("quynhnguyen", "Nguyen Thi Quynh", "0902000015", "quynh.nguyen@email.com", "GOLD", CustomerStatus.ACTIVE, 31, 6800000, 3100, 18)
        );

        for (DemoCustomerSeed seed : seeds) {
            customerRepository.findByPhoneNumber(seed.phone()).ifPresentOrElse(
                    existing -> updateDemoCustomer(existing, seed, tiers.get(seed.tierName())),
                    () -> createDemoCustomer(seed, tiers.get(seed.tierName()))
            );
        }
    }

    private void createDemoCustomer(DemoCustomerSeed seed, LoyaltyTier tier) {
        customerRepository.save(Customer.builder()
                .username(seed.username())
                .fullName(seed.fullName())
                .phoneNumber(seed.phone())
                .email(seed.email())
                .authProvider(CustomerAuthProvider.PHONE)
                .status(seed.status())
                .passwordHash(passwordEncoder.encode("Customer@123"))
                .tier(tier)
                .visitCount(seed.visits())
                .totalSpending(new BigDecimal(seed.spending()))
                .tierSpending(new BigDecimal(seed.spending()))
                .loyaltyPoints(seed.points())
                .lastCompletedBookingAt(seed.visits() > 0 ? LocalDateTime.now().minusDays(seed.lastVisitDaysAgo()) : null)
                .build());
    }

    private void updateDemoCustomer(Customer customer, DemoCustomerSeed seed, LoyaltyTier tier) {
        if (customer.getUsername() == null || customer.getUsername().isBlank()) {
            customer.setUsername(seed.username());
        }
        customer.setFullName(seed.fullName());
        customer.setEmail(seed.email());
        customer.setStatus(seed.status());
        customer.setTier(tier);
        customer.setVisitCount(seed.visits());
        customer.setTotalSpending(new BigDecimal(seed.spending()));
        customer.setTierSpending(new BigDecimal(seed.spending()));
        customer.setLoyaltyPoints(seed.points());
        customer.setLastCompletedBookingAt(seed.visits() > 0 ? LocalDateTime.now().minusDays(seed.lastVisitDaysAgo()) : null);
        customerRepository.save(customer);
    }

    private static final List<DemoVehicleSeed> DEMO_VEHICLES = List.of(
            new DemoVehicleSeed("0902000001", "51A-12345", "Honda SH 150i"),
            new DemoVehicleSeed("0902000002", "51B-67890", "Yamaha Exciter 150"),
            new DemoVehicleSeed("0902000003", "30C-11223", "Vespa GTS 300"),
            new DemoVehicleSeed("0902000004", "43D-44556", "Honda Air Blade"),
            new DemoVehicleSeed("0902000005", "59E-77889", "Honda Vision"),
            new DemoVehicleSeed("0902000006", "77F-99001", "Yamaha NVX 155"),
            new DemoVehicleSeed("0902000007", "92G-22334", "Honda SH Mode"),
            new DemoVehicleSeed("0902000008", "61H-55667", "Honda Lead"),
            new DemoVehicleSeed("0902000009", "29K-99999", "Honda SH 350i"),
            new DemoVehicleSeed("0902000010", "30M-88888", "Yamaha Sirius"),
            new DemoVehicleSeed("0902000011", "59N-77777", "Vespa LX 125"),
            new DemoVehicleSeed("0902000012", "43P-66666", "Suzuki Raider 150"),
            new DemoVehicleSeed("0902000013", "36Q-55555", "Honda Wave Alpha"),
            new DemoVehicleSeed("0902000014", "75R-44444", "Yamaha Grande"),
            new DemoVehicleSeed("0902000015", "86S-33333", "Honda PCX 160")
    );

    private void seedDemoVehicles() {
        for (DemoVehicleSeed seed : DEMO_VEHICLES) {
            if (vehicleRepository.existsByLicensePlateIgnoreCase(seed.licensePlate())) {
                continue;
            }
            customerRepository.findByPhoneNumber(seed.phone()).ifPresentOrElse(
                    customer -> vehicleRepository.save(Vehicle.builder()
                            .customer(customer)
                            .licensePlate(seed.licensePlate())
                            .model(seed.model())
                            .build()),
                    () -> log.warn("Customer with phone {} not found — skip vehicle {}",
                            seed.phone(), seed.licensePlate())
            );
        }
    }

    private void seedServiceCatalog() {
        if (serviceCatalogRepository.count() > 0) return;
        List<ServiceCatalog> services = List.of(
                ServiceCatalog.builder().serviceCode("PKG-STD").serviceName("Rửa xe máy tiêu chuẩn").serviceType(ServiceType.PACKAGE).price(new BigDecimal("30000.00")).durationMinutes(15).description("Rửa bọt tuyết chuyên dụng, xịt khô, lau bóng").isActive(true).displayOrder(1).build(),
                ServiceCatalog.builder().serviceCode("PKG-DELUXE").serviceName("Rửa xe máy cao cấp").serviceType(ServiceType.PACKAGE).price(new BigDecimal("50000.00")).durationMinutes(25).description("Rửa bọt tuyết, tẩy nhờn lốc máy, dưỡng bóng lốp").isActive(true).displayOrder(2).build(),
                ServiceCatalog.builder().serviceCode("PKG-ULTIMATE").serviceName("Rửa xe máy siêu cấp & bảo dưỡng").serviceType(ServiceType.PACKAGE).price(new BigDecimal("80000.00")).durationMinutes(40).description("Rửa chi tiết toàn diện, tẩy ố xích chíp, dưỡng nhựa nhám, tra dầu xích").isActive(true).displayOrder(3).build(),
                ServiceCatalog.builder().serviceCode("ADD-CHAIN").serviceName("Tẩy rửa và dưỡng xích (sên)").serviceType(ServiceType.ADDON).price(new BigDecimal("20000.00")).durationMinutes(10).description("Tẩy sạch cặn bẩn xích, tra dầu bôi trơn chuyên dụng").isActive(true).displayOrder(4).build(),
                ServiceCatalog.builder().serviceCode("ADD-HELMET").serviceName("Vệ sinh mũ bảo hiểm khử khuẩn").serviceType(ServiceType.ADDON).price(new BigDecimal("15000.00")).durationMinutes(10).description("Khử mùi bọt nano, sấy khô mũ bảo hiểm").isActive(true).displayOrder(5).build()
        );
        serviceCatalogRepository.saveAll(services);
    }

    private void seedTimeSlots() {
        if (timeSlotRepository.count() > 0) return;
        List<TimeSlot> slots = List.of(
                TimeSlot.builder().startTime(LocalTime.of(7, 30)).endTime(LocalTime.of(8, 0)).maxCapacity(3).isActive(true).displayOrder(1).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(8, 30)).maxCapacity(3).isActive(true).displayOrder(2).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(8, 30)).endTime(LocalTime.of(9, 0)).maxCapacity(3).isActive(true).displayOrder(3).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxCapacity(3).isActive(true).displayOrder(4).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxCapacity(3).isActive(true).displayOrder(5).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxCapacity(3).isActive(true).displayOrder(6).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxCapacity(3).isActive(true).displayOrder(7).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxCapacity(3).isActive(true).displayOrder(8).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(11, 30)).endTime(LocalTime.of(12, 0)).maxCapacity(3).isActive(true).displayOrder(9).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(12, 30)).maxCapacity(3).isActive(true).displayOrder(10).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(12, 30)).endTime(LocalTime.of(13, 0)).maxCapacity(3).isActive(true).displayOrder(11).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(13, 30)).maxCapacity(3).isActive(true).displayOrder(12).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(13, 30)).endTime(LocalTime.of(14, 0)).maxCapacity(3).isActive(true).displayOrder(13).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(14, 30)).maxCapacity(3).isActive(true).displayOrder(14).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(14, 30)).endTime(LocalTime.of(15, 0)).maxCapacity(3).isActive(true).displayOrder(15).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(15, 30)).maxCapacity(3).isActive(true).displayOrder(16).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(15, 30)).endTime(LocalTime.of(16, 0)).maxCapacity(3).isActive(true).displayOrder(17).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(16, 30)).maxCapacity(3).isActive(true).displayOrder(18).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(16, 30)).endTime(LocalTime.of(17, 0)).maxCapacity(3).isActive(true).displayOrder(19).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(17, 30)).maxCapacity(3).isActive(true).displayOrder(20).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(17, 30)).endTime(LocalTime.of(18, 0)).maxCapacity(3).isActive(true).displayOrder(21).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(18, 30)).maxCapacity(3).isActive(true).displayOrder(22).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(18, 30)).endTime(LocalTime.of(19, 0)).maxCapacity(3).isActive(true).displayOrder(23).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(19, 30)).maxCapacity(3).isActive(true).displayOrder(24).dayOfWeek("ALL").build()
        );
        timeSlotRepository.saveAll(slots);
    }


    private void seedDemoBookings() {
        if (bookingRepository.count() > 0) {
            LocalDate today = LocalDate.now();
            bookingRepository.findByBookingCode("NV-1001").ifPresent(b -> { if (!today.equals(b.getBookingDate())) { b.setBookingDate(today); bookingRepository.save(b); } });
            bookingRepository.findByBookingCode("NV-1002").ifPresent(b -> { if (!today.equals(b.getBookingDate()) || b.getStatus() != BookingStatus.COMPLETED) { b.setBookingDate(today); b.setStatus(BookingStatus.COMPLETED); b.setPaymentStatus(PaymentStatus.PAID); bookingRepository.save(b); } });
            bookingRepository.findByBookingCode("NV-1003").ifPresent(b -> { if (!today.equals(b.getBookingDate())) { b.setBookingDate(today); bookingRepository.save(b); } });
            bookingRepository.findByBookingCode("NV-1004").ifPresent(b -> { if (!today.plusDays(1).equals(b.getBookingDate())) { b.setBookingDate(today.plusDays(1)); bookingRepository.save(b); } });
            return;
        }

        List<ServiceCatalog> allServices = serviceCatalogRepository.findAll();
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        if (allServices.isEmpty() || allSlots.isEmpty()) return;

        ServiceCatalog pkgStd = allServices.stream().filter(s -> "PKG-STD".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog pkgDeluxe = allServices.stream().filter(s -> "PKG-DELUXE".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog pkgUltimate = allServices.stream().filter(s -> "PKG-ULTIMATE".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog addHelmet = allServices.stream().filter(s -> "ADD-HELMET".equals(s.getServiceCode())).findFirst().orElse(null);
        ServiceCatalog addChain = allServices.stream().filter(s -> "ADD-CHAIN".equals(s.getServiceCode())).findFirst().orElse(null);

        TimeSlot slot8_9 = allSlots.stream().filter(s -> s.getDisplayOrder() == 2).findFirst().orElse(allSlots.get(0));
        TimeSlot slot9_10 = allSlots.stream().filter(s -> s.getDisplayOrder() == 4).findFirst().orElse(allSlots.get(0));
        TimeSlot slot10_11 = allSlots.stream().filter(s -> s.getDisplayOrder() == 6).findFirst().orElse(allSlots.get(0));
        TimeSlot slot14_15 = allSlots.stream().filter(s -> s.getDisplayOrder() == 14).findFirst().orElse(allSlots.get(0));
        TimeSlot slot15_16 = allSlots.stream().filter(s -> s.getDisplayOrder() == 16).findFirst().orElse(allSlots.get(0));
        TimeSlot slot16_17 = allSlots.stream().filter(s -> s.getDisplayOrder() == 18).findFirst().orElse(allSlots.get(0));

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<BookingSeedInfo> bookingSeeds = List.of(
            new BookingSeedInfo("NV-1001", "0902000001", "51A-12345", "Honda SH 150i", today, slot8_9, BookingStatus.PENDING, PaymentStatus.UNPAID, List.of(pkgStd, addHelmet), "Khách dặn rửa kỹ lốp xe"),
            new BookingSeedInfo("NV-1002", "0902000002", "51B-67890", "Yamaha Exciter 150", today, slot9_10, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgDeluxe), "Khách hẹn đến đúng giờ"),
            new BookingSeedInfo("NV-1003", "0902000003", "30C-11223", "Vespa GTS 300", today, slot10_11, BookingStatus.IN_PROGRESS, PaymentStatus.PAID, List.of(pkgUltimate), "Đang rửa chi tiết"),
            new BookingSeedInfo("NV-1004", "0902000004", "43D-44556", "Honda Air Blade", tomorrow, slot14_15, BookingStatus.CANCELLED_BY_CUSTOMER, PaymentStatus.UNPAID, List.of(pkgDeluxe), "Khách bận đột xuất nên hủy"),
            new BookingSeedInfo("NV-1005", "0902000002", "51B-67890", "Yamaha Exciter 150", today.minusDays(3), slot9_10, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgDeluxe, addChain), "Khách quen trạm"),
            new BookingSeedInfo("NV-1006", "0902000003", "30C-11223", "Vespa GTS 300", today.minusDays(5), slot10_11, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgUltimate), "Khách đánh giá gầm dơ"),
            new BookingSeedInfo("NV-1007", "0902000004", "43D-44556", "Honda Air Blade", today.minusDays(1), slot8_9, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgDeluxe, addHelmet), "Rất sạch sẽ"),
            new BookingSeedInfo("NV-1008", "0902000005", "59E-77889", "Honda Vision", today.minusDays(15), slot14_15, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgStd), "Khách hài lòng"),
            new BookingSeedInfo("NV-1009", "0902000006", "77F-99001", "Yamaha NVX 155", today.minusDays(25), slot15_16, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgDeluxe), "Bình thường"),
            new BookingSeedInfo("NV-1010", "0902000008", "61H-55667", "Honda Lead", today.minusDays(8), slot16_17, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgStd), "Nhanh chóng"),
            new BookingSeedInfo("NV-1011", "0902000011", "75R-44444", "Yamaha Grande", today.minusDays(12), slot9_10, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgDeluxe), "Dịch vụ ổn"),
            new BookingSeedInfo("NV-1012", "0902000012", "86S-33333", "Honda PCX 160", today.minusDays(4), slot10_11, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgUltimate, addChain), "Sạch đẹp"),
            new BookingSeedInfo("NV-1013", "0902000013", "36Q-55555", "Honda Wave Alpha", today, slot15_16, BookingStatus.CONFIRMED, PaymentStatus.UNPAID, List.of(pkgStd), "Đợi rửa"),
            new BookingSeedInfo("NV-1014", "0902000014", "75R-44444", "Yamaha Grande", tomorrow, slot8_9, BookingStatus.PENDING, PaymentStatus.UNPAID, List.of(pkgDeluxe), "Đã cọc"),
            new BookingSeedInfo("NV-1015", "0902000015", "86S-33333", "Honda PCX 160", today.minusDays(18), slot14_15, BookingStatus.COMPLETED, PaymentStatus.PAID, List.of(pkgUltimate), "Tốt")
        );

        for (BookingSeedInfo seed : bookingSeeds) {
            customerRepository.findByPhoneNumber(seed.phone()).ifPresent(cus -> {
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (ServiceCatalog sc : seed.services()) {
                    if (sc != null) {
                        totalAmount = totalAmount.add(sc.getPrice());
                    }
                }
                Booking b = Booking.builder()
                        .bookingCode(seed.code())
                        .customer(cus)
                        .licensePlate(seed.licensePlate())
                        .model(seed.model())
                        .bookingDate(seed.date())
                        .timeSlot(seed.slot())
                        .status(seed.status())
                        .paymentStatus(seed.paymentStatus())
                        .totalEstimatedAmount(totalAmount)
                        .notes(seed.notes())
                        .build();

                for (ServiceCatalog sc : seed.services()) {
                    if (sc != null) {
                        b.addItem(createBookingItem(sc));
                    }
                }
                bookingRepository.save(b);
            });
        }
    }

    private BookingItem createBookingItem(ServiceCatalog sc) {
        return BookingItem.builder()
                .serviceId(sc.getServiceId())
                .serviceCodeSnapshot(sc.getServiceCode())
                .serviceNameSnapshot(sc.getServiceName())
                .serviceTypeSnapshot(sc.getServiceType())
                .priceSnapshot(sc.getPrice())
                .build();
    }

    private void seedDemoNotifications() {
        if (notificationRepository.count() > 0) return;

        List<Notification> notifications = new ArrayList<>();

        // Customer 0902000001 (Tran Van An)
        customerRepository.findByPhoneNumber("0902000001").ifPresent(cus -> {
            notifications.add(Notification.builder()
                    .recipientType(NotificationRecipientType.CUSTOMER)
                    .recipientId(cus.getCustomerId())
                    .title("🎉 Đặt lịch thành công!")
                    .content("Mã đơn NV-1001 cho khung giờ 08:00 - 09:00 ngày hôm nay đã được ghi nhận.")
                    .type(NotificationType.NEW_BOOKING)
                    .referenceCode("NV-1001")
                    .isRead(false)
                    .build());
            notifications.add(Notification.builder()
                    .recipientType(NotificationRecipientType.CUSTOMER)
                    .recipientId(cus.getCustomerId())
                    .title("👑 Chào mừng thành viên mới!")
                    .content("Bạn đã chính thức trở thành thành viên Member của AutoWash Pro.")
                    .type(NotificationType.SYSTEM_ALERT)
                    .referenceCode("WELCOME")
                    .isRead(true)
                    .build());
        });

        // Customer 0902000002 (Le Thi Mai)
        customerRepository.findByPhoneNumber("0902000002").ifPresent(cus -> {
            notifications.add(Notification.builder()
                    .recipientType(NotificationRecipientType.CUSTOMER)
                    .recipientId(cus.getCustomerId())
                    .title("🎉 Đặt lịch thành công!")
                    .content("Mã đơn NV-1002 cho khung giờ 09:00 - 10:00 ngày hôm nay đã được tiếp nhận.")
                    .type(NotificationType.NEW_BOOKING)
                    .referenceCode("NV-1002")
                    .isRead(true)
                    .build());
            notifications.add(Notification.builder()
                    .recipientType(NotificationRecipientType.CUSTOMER)
                    .recipientId(cus.getCustomerId())
                    .title("✅ Xác nhận lịch hẹn!")
                    .content("Lịch hẹn NV-1002 của bạn đã được Admin xác nhận. Vui lòng đến đúng giờ!")
                    .type(NotificationType.BOOKING_CONFIRMED)
                    .referenceCode("NV-1002")
                    .isRead(false)
                    .build());
        });

        // Staff / Admin notifications (ALL_STAFF)
        notifications.add(Notification.builder()
                .recipientType(NotificationRecipientType.ALL_STAFF)
                .title("🎉 Đơn đặt lịch mới!")
                .content("Khách Trần Văn An đặt khung 08:00 - 09:00 ngày hôm nay (Biển số: 29A-12345)")
                .type(NotificationType.NEW_BOOKING)
                .referenceCode("NV-1001")
                .isRead(false)
                .build());
        notifications.add(Notification.builder()
                .recipientType(NotificationRecipientType.ALL_STAFF)
                .title("🎉 Đơn đặt lịch mới!")
                .content("Khách Lê Thị Mai đặt khung 09:00 - 10:00 ngày hôm nay (Biển số: 51B-67890)")
                .type(NotificationType.NEW_BOOKING)
                .referenceCode("NV-1002")
                .isRead(false)
                .build());
        notifications.add(Notification.builder()
                .recipientType(NotificationRecipientType.ALL_STAFF)
                .title("⚠️ Khách hàng hủy lịch hẹn!")
                .content("Khách Nguyễn Hoàng Yến đã hủy lịch hẹn NV-1004 cho khung giờ 14:00 - 15:00 ngày mai.")
                .type(NotificationType.BOOKING_CANCELLED)
                .referenceCode("NV-1004")
                .isRead(true)
                .build());

        notificationRepository.saveAll(notifications);
    }

    private void seedDemoPromotionsAndFeedbacks() {
        if (promotionRepository.count() == 0) {
            log.info("Seeding demo promotions for E2E-3...");
            List<Promotion> promotions = List.of(
                    Promotion.builder()
                            .code("WELCOME50")
                            .name("Quà chào mừng thành viên mới")
                            .description("Giảm 10% cho đơn rửa xe đầu tiên")
                            .discountType(DiscountType.PERCENTAGE)
                            .value(BigDecimal.valueOf(10))
                            .costPoints(0)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(1000)
                            .issuedCount(15)
                            .redeemedCount(8)
                            .startDate(LocalDateTime.now().minusDays(10))
                            .endDate(LocalDateTime.now().plusMonths(3))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("SUMMER24")
                            .name("Voucher Mùa Hè Rực Rỡ")
                            .description("Giảm giá 50.000đ cho tất cả dịch vụ rửa xe và chăm sóc chi tiết")
                            .discountType(DiscountType.FIXED_AMOUNT)
                            .value(BigDecimal.valueOf(50000))
                            .costPoints(0)
                            .minTier("Gold")
                            .minRecencyDays(0)
                            .totalBudget(500)
                            .issuedCount(120)
                            .redeemedCount(85)
                            .startDate(LocalDateTime.now().minusDays(5))
                            .endDate(LocalDateTime.now().plusMonths(1))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("FREEWASH")
                            .name("Tri ân khách hàng Kim Cương")
                            .description("Rửa xe toàn diện miễn phí 100% dành cho thành viên Platinum")
                            .discountType(DiscountType.FREE_SERVICE)
                            .value(BigDecimal.valueOf(100000))
                            .costPoints(0)
                            .minTier("Platinum")
                            .minRecencyDays(0)
                            .totalBudget(100)
                            .issuedCount(25)
                            .redeemedCount(20)
                            .startDate(LocalDateTime.now().minusDays(15))
                            .endDate(LocalDateTime.now().plusMonths(2))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("VOUCHER_50K")
                            .name("Voucher Giảm Giá 50k đổi điểm")
                            .description("Áp dụng giảm trực tiếp cho mọi hóa đơn đặt lịch rửa xe hoặc dịch vụ phụ trợ.")
                            .discountType(DiscountType.FIXED_AMOUNT)
                            .value(BigDecimal.valueOf(50000))
                            .costPoints(450)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(2000)
                            .issuedCount(310)
                            .redeemedCount(190)
                            .startDate(LocalDateTime.now().minusDays(30))
                            .endDate(LocalDateTime.now().plusMonths(6))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("VOUCHER_FREE")
                            .name("Voucher Rửa Xe Miễn Phí (Đổi Điểm)")
                            .description("Đổi 1 lượt sử dụng gói rửa xe toàn diện hoàn toàn miễn phí.")
                            .discountType(DiscountType.FREE_SERVICE)
                            .value(BigDecimal.valueOf(100000))
                            .costPoints(1000)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(500)
                            .issuedCount(80)
                            .redeemedCount(45)
                            .startDate(LocalDateTime.now().minusDays(20))
                            .endDate(LocalDateTime.now().plusMonths(6))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("PAUSED_CAMPAIGN")
                            .name("Chiến dịch tạm ngưng chạy thử")
                            .description("Chiến dịch giảm giá 20k đang tạm ngưng.")
                            .discountType(DiscountType.FIXED_AMOUNT)
                            .value(BigDecimal.valueOf(20000))
                            .costPoints(0)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(200)
                            .issuedCount(10)
                            .redeemedCount(2)
                            .startDate(LocalDateTime.now().minusDays(5))
                            .endDate(LocalDateTime.now().plusMonths(1))
                            .status(PromotionStatus.PAUSED)
                            .build(),
                    Promotion.builder()
                            .code("EXPIRED_CAMPAIGN")
                            .name("Khuyến mãi hết hạn từ lâu")
                            .description("Khuyến mãi giảm 15% mùa đông trước.")
                            .discountType(DiscountType.PERCENTAGE)
                            .value(BigDecimal.valueOf(15))
                            .costPoints(0)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(100)
                            .issuedCount(50)
                            .redeemedCount(48)
                            .startDate(LocalDateTime.now().minusMonths(6))
                            .endDate(LocalDateTime.now().minusMonths(3))
                            .status(PromotionStatus.EXPIRED)
                            .build(),
                    Promotion.builder()
                            .code("COMPENSATE50")
                            .name("Voucher Đền Bù Tạ Lỗi CSKH")
                            .description("Voucher đền bù trải nghiệm dịch vụ chưa hài lòng.")
                            .discountType(DiscountType.FIXED_AMOUNT)
                            .value(BigDecimal.valueOf(50000))
                            .costPoints(0)
                            .minTier("Member")
                            .minRecencyDays(0)
                            .totalBudget(1000)
                            .issuedCount(5)
                            .redeemedCount(2)
                            .startDate(LocalDateTime.now().minusDays(10))
                            .endDate(LocalDateTime.now().plusMonths(3))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("WINTER_COMBO")
                            .name("Voucher Combo Mùa Đông")
                            .description("Giảm giá 20% cho thành viên từ Silver trở lên.")
                            .discountType(DiscountType.PERCENTAGE)
                            .value(BigDecimal.valueOf(20))
                            .costPoints(500)
                            .minTier("Silver")
                            .minRecencyDays(0)
                            .totalBudget(300)
                            .issuedCount(12)
                            .redeemedCount(3)
                            .startDate(LocalDateTime.now().minusDays(2))
                            .endDate(LocalDateTime.now().plusMonths(2))
                            .status(PromotionStatus.ACTIVE)
                            .build(),
                    Promotion.builder()
                            .code("VIP_SPECIAL")
                            .name("Quà tặng VIP vắng mặt lâu ngày")
                            .description("Ưu đãi đặc biệt giảm 100k cho khách Platinum không đến trên 30 ngày.")
                            .discountType(DiscountType.FIXED_AMOUNT)
                            .value(BigDecimal.valueOf(100000))
                            .costPoints(0)
                            .minTier("Platinum")
                            .minRecencyDays(30)
                            .totalBudget(100)
                            .issuedCount(1)
                            .redeemedCount(0)
                            .startDate(LocalDateTime.now().minusDays(3))
                            .endDate(LocalDateTime.now().plusMonths(1))
                            .status(PromotionStatus.ACTIVE)
                            .build()
            );
            promotionRepository.saveAll(promotions);

            // Seed Wallets for Customers
            seedCustomerWallets();
        }

        if (customerFeedbackRepository.count() == 0) {
            log.info("Seeding demo customer feedbacks for E2E-3...");
            seedCustomerFeedbacks();
            log.info("Seeded demo customer feedbacks successfully!");
        }
    }

    private void seedCustomerWallets() {
        Promotion pWelcome = promotionRepository.findByCode("WELCOME50").orElse(null);
        Promotion p50k = promotionRepository.findByCode("VOUCHER_50K").orElse(null);
        Promotion pSummer = promotionRepository.findByCode("SUMMER24").orElse(null);
        Promotion pFree = promotionRepository.findByCode("VOUCHER_FREE").orElse(null);
        Promotion pFreeWash = promotionRepository.findByCode("FREEWASH").orElse(null);
        Promotion pVipSpecial = promotionRepository.findByCode("VIP_SPECIAL").orElse(null);

        // Customer 1: Nguyen Van An
        customerRepository.findByPhoneNumber("0902000001").ifPresent(c -> {
            if (pWelcome != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pWelcome).voucherCode("VOU-WELCOME-0001")
                        .issuedAt(LocalDateTime.now().minusDays(2)).expiryDate(LocalDateTime.now().plusDays(12))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.CLAIM).build());
            }
            if (p50k != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(p50k).voucherCode("VOU-50K-0001")
                        .issuedAt(LocalDateTime.now().minusDays(5)).expiryDate(LocalDateTime.now().plusDays(25))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.EXCHANGE).build());
            }
        });

        // Customer 2: Tran Thi Binh
        customerRepository.findByPhoneNumber("0902000002").ifPresent(c -> {
            if (pSummer != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pSummer).voucherCode("VOU-SUMMER-0002")
                        .issuedAt(LocalDateTime.now().minusDays(4)).expiryDate(LocalDateTime.now().plusDays(10))
                        .status(CustomerPromotionStatus.USED).source(CustomerPromotionSource.CLAIM).build());
            }
            if (p50k != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(p50k).voucherCode("VOU-50K-0002")
                        .issuedAt(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(29))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.EXCHANGE).build());
            }
        });

        // Customer 3: Le Minh Cuong
        customerRepository.findByPhoneNumber("0902000003").ifPresent(c -> {
            if (pFree != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pFree).voucherCode("VOU-FREE-0003")
                        .issuedAt(LocalDateTime.now().minusDays(3)).expiryDate(LocalDateTime.now().plusDays(27))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.EXCHANGE).build());
            }
            if (pWelcome != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pWelcome).voucherCode("VOU-WELCOME-0003")
                        .issuedAt(LocalDateTime.now().minusDays(8)).expiryDate(LocalDateTime.now().plusDays(2))
                        .status(CustomerPromotionStatus.USED).source(CustomerPromotionSource.CLAIM).build());
            }
        });

        // Customer 4: Pham Thi Dung
        customerRepository.findByPhoneNumber("0902000004").ifPresent(c -> {
            if (pFreeWash != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pFreeWash).voucherCode("VOU-FREEWASH-0004")
                        .issuedAt(LocalDateTime.now().minusDays(1)).expiryDate(LocalDateTime.now().plusDays(14))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.CLAIM).build());
            }
            if (pSummer != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pSummer).voucherCode("VOU-SUMMER-0004")
                        .issuedAt(LocalDateTime.now().minusDays(2)).expiryDate(LocalDateTime.now().plusDays(12))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.CLAIM).build());
            }
            if (pWelcome != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pWelcome).voucherCode("VOU-WELCOME-0004")
                        .issuedAt(LocalDateTime.now().minusDays(15)).expiryDate(LocalDateTime.now().minusDays(5))
                        .status(CustomerPromotionStatus.EXPIRED).source(CustomerPromotionSource.CLAIM).build());
            }
        });

        // Customer 9: Nguyen Van Khanh
        customerRepository.findByPhoneNumber("0902000009").ifPresent(c -> {
            if (pVipSpecial != null) {
                customerPromotionRepository.save(CustomerPromotion.builder()
                        .customer(c).promotion(pVipSpecial).voucherCode("VOU-VIP-0009")
                        .issuedAt(LocalDateTime.now().minusDays(3)).expiryDate(LocalDateTime.now().plusDays(27))
                        .status(CustomerPromotionStatus.ISSUED).source(CustomerPromotionSource.GIFT_DIRECT).build());
            }
        });
    }

    private void seedCustomerFeedbacks() {
        // Customer 1: Nguyen Van An
        customerRepository.findByPhoneNumber("0902000001").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1002").serviceName("Gói Rửa xe máy cao cấp")
                    .ratingStars(5).comment("Rửa rất sạch và chu đáo. Nhân viên thái độ tốt.")
                    .createdAt(LocalDateTime.now().minusDays(3)).status(FeedbackStatus.RESOLVED)
                    .resolutionNotes("AI Sentiment: Tích cực. Ghi nhận đánh giá tốt.").build());
        });

        // Customer 2: Tran Thi Binh
        customerRepository.findByPhoneNumber("0902000002").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1005").serviceName("Gói Rửa xe máy cao cấp + Tẩy xích")
                    .ratingStars(2).comment("Lau chưa sạch phần gầm xe, còn bám bẩn nhiều dưới chắn bùn.")
                    .createdAt(LocalDateTime.now().minusDays(2)).status(FeedbackStatus.NEW).build());
        });

        // Customer 3: Le Minh Cuong
        customerRepository.findByPhoneNumber("0902000003").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1006").serviceName("Gói Rửa xe máy siêu cấp")
                    .ratingStars(1).comment("Đã đặt lịch hẹn lúc 10h mà tới nơi bắt đợi gần 40 phút. Trạm làm ăn tắc trách quá!")
                    .createdAt(LocalDateTime.now().minusDays(5)).status(FeedbackStatus.NEW).build());
        });

        // Customer 4: Pham Thi Dung
        customerRepository.findByPhoneNumber("0902000004").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1007").serviceName("Gói Rửa xe máy cao cấp")
                    .ratingStars(5).comment("Gói Premium rửa siêu sạch, anh kỹ thuật viên còn bôi mỡ xích miễn phí hỗ trợ.")
                    .createdAt(LocalDateTime.now().minusDays(1)).status(FeedbackStatus.RESOLVED)
                    .resolutionNotes("AI Sentiment: Tích cực. Khách hàng thân thiết Platinum.").build());
        });

        // Customer 5: Hoang Van Em
        customerRepository.findByPhoneNumber("0902000005").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1008").serviceName("Gói Rửa xe máy tiêu chuẩn")
                    .ratingStars(3).comment("Rửa tạm ổn nhưng phòng chờ hôm nay nóng quá, máy lọc nước thì hết nước.")
                    .createdAt(LocalDateTime.now().minusDays(15)).status(FeedbackStatus.NEW).build());
        });

        // Customer 6: Vo Thi Phuong
        customerRepository.findByPhoneNumber("0902000006").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1009").serviceName("Gói Rửa xe máy cao cấp")
                    .ratingStars(2).comment("Lần trước rửa xe nẹp cửa bên hông bị trầy xước nhẹ.")
                    .createdAt(LocalDateTime.now().minusDays(25)).status(FeedbackStatus.RESOLVED)
                    .resolutionNotes("Đã gọi điện xin lỗi và đền bù mã voucher giảm giá 50k. Khách đã vui vẻ đồng ý.").build());
        });

        // Customer 8: Bui Thi Hoa
        customerRepository.findByPhoneNumber("0902000008").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1010").serviceName("Gói Rửa xe máy tiêu chuẩn")
                    .ratingStars(4).comment("Dịch vụ tốt, rửa nhanh gọn lẹ.")
                    .createdAt(LocalDateTime.now().minusDays(8)).status(FeedbackStatus.RESOLVED)
                    .resolutionNotes("Đánh giá tốt từ khách hạng Gold.").build());
        });

        // Customer 11: Le Thi Mai
        customerRepository.findByPhoneNumber("0902000011").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1011").serviceName("Gói Rửa xe máy cao cấp")
                    .ratingStars(1).comment("Vệ sinh mũ bảo hiểm sấy nano xong vẫn còn mùi ẩm hôi, chưa sấy khô hoàn toàn.")
                    .createdAt(LocalDateTime.now().minusDays(12)).status(FeedbackStatus.NEW).build());
        });

        // Customer 12: Pham Hoang Nam
        customerRepository.findByPhoneNumber("0902000012").ifPresent(c -> {
            customerFeedbackRepository.save(CustomerFeedback.builder()
                    .customer(c).bookingId("NV-1012").serviceName("Gói Rửa xe máy siêu cấp")
                    .ratingStars(5).comment("Rửa siêu sạch, dưỡng nhựa nhám và lốp rất đều tay, xe đi như mới.")
                    .createdAt(LocalDateTime.now().minusDays(4)).status(FeedbackStatus.RESOLVED)
                    .resolutionNotes("Phản hồi rất tốt. Khách hàng thân thiết Gold.").build());
        });
    }

    private void seedPointTransactions() {
        log.info("Seeding point transactions...");
        
        // Customer 1: Nguyen Van An (0902000001) - 850 Pts
        customerRepository.findByPhoneNumber("0902000001").ifPresent(c -> {
            savePointTx(c, 735, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(15));
            savePointTx(c, 50, PointActivityType.EARNED, "NV-1002", LocalDateTime.now().minusDays(1));
            savePointTx(c, 80, PointActivityType.EARNED, "NV-1016", LocalDateTime.now().minusDays(5));
            savePointTx(c, -15, PointActivityType.PENALTY, "NV-1017", LocalDateTime.now().minusDays(8));
        });

        // Customer 2: Tran Thi Binh (0902000002) - 2100 Pts
        customerRepository.findByPhoneNumber("0902000002").ifPresent(c -> {
            savePointTx(c, 2480, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(20));
            savePointTx(c, 70, PointActivityType.EARNED, "NV-1005", LocalDateTime.now().minusDays(3));
            savePointTx(c, -450, PointActivityType.REDEEMED, null, LocalDateTime.now().minusDays(10));
        });

        // Customer 3: Le Minh Cuong (0902000003) - 4800 Pts
        customerRepository.findByPhoneNumber("0902000003").ifPresent(c -> {
            savePointTx(c, 4800, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(10));
        });

        // Customer 4: Pham Thi Dung (0902000004) - 9200 Pts
        customerRepository.findByPhoneNumber("0902000004").ifPresent(c -> {
            savePointTx(c, 9890, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(25));
            savePointTx(c, 160, PointActivityType.EARNED, "NV-1007", LocalDateTime.now().minusDays(1));
            savePointTx(c, -1000, PointActivityType.REDEEMED, null, LocalDateTime.now().minusDays(5));
            savePointTx(c, 200, PointActivityType.EARNED, "NV-1015", LocalDateTime.now().minusDays(18));
            savePointTx(c, -50, PointActivityType.EXPIRY, null, LocalDateTime.now().minusDays(30));
        });

        // Customer 5: Hoang Van Em (0902000005) - 120 Pts
        customerRepository.findByPhoneNumber("0902000005").ifPresent(c -> {
            savePointTx(c, 120, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(15));
        });

        // Customer 6: Vo Thi Phuong (0902000006) - 900 Pts
        customerRepository.findByPhoneNumber("0902000006").ifPresent(c -> {
            savePointTx(c, 900, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(25));
        });

        // Customer 7: Dang Quoc Giang (0902000007) - 40 Pts
        customerRepository.findByPhoneNumber("0902000007").ifPresent(c -> {
            savePointTx(c, 40, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(5));
        });

        // Customer 8: Bui Thi Hoa (0902000008) - 3500 Pts
        customerRepository.findByPhoneNumber("0902000008").ifPresent(c -> {
            savePointTx(c, 3500, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(8));
        });

        // Customer 9: Nguyen Van Khanh (0902000009) - 6000 Pts
        customerRepository.findByPhoneNumber("0902000009").ifPresent(c -> {
            savePointTx(c, 6000, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(5));
        });

        // Customer 10: Tran Van Lam (0902000010) - 300 Pts
        customerRepository.findByPhoneNumber("0902000010").ifPresent(c -> {
            savePointTx(c, 300, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(30));
        });

        // Customer 11: Le Thi Mai (0902000011) - 1800 Pts
        customerRepository.findByPhoneNumber("0902000011").ifPresent(c -> {
            savePointTx(c, 1800, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(12));
        });

        // Customer 12: Pham Hoang Nam (0902000012) - 4200 Pts
        customerRepository.findByPhoneNumber("0902000012").ifPresent(c -> {
            savePointTx(c, 4200, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(4));
        });

        // Customer 13: Tran Thi Oanh (0902000013) - 10 Pts
        customerRepository.findByPhoneNumber("0902000013").ifPresent(c -> {
            savePointTx(c, 10, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(1));
        });

        // Customer 14: Le Huu Phuc (0902000014) - 1100 Pts
        customerRepository.findByPhoneNumber("0902000014").ifPresent(c -> {
            savePointTx(c, 1100, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(15));
        });

        // Customer 15: Nguyen Thi Quynh (0902000015) - 3100 Pts
        customerRepository.findByPhoneNumber("0902000015").ifPresent(c -> {
            savePointTx(c, 3100, PointActivityType.EARNED, "NV-BASE", LocalDateTime.now().minusDays(18));
        });
    }

    private void savePointTx(Customer customer, int points, PointActivityType type, String bookingCode, LocalDateTime date) {
        PointTransaction tx = PointTransaction.builder()
                .customer(customer)
                .points(points)
                .activityType(type)
                .bookingCode(bookingCode)
                .build();
        tx.setCreatedAt(date);
        tx.setUpdatedAt(date);
        pointTransactionRepository.save(tx);
    }


    private record DemoVehicleSeed(
            String phone,
            String licensePlate,
            String model
    ) {}

    private record DemoCustomerSeed(
            String username,
            String fullName,
            String phone,
            String email,
            String tierName,
            CustomerStatus status,
            int visits,
            long spending,
            int points,
            int lastVisitDaysAgo
    ) {}

    private record BookingSeedInfo(
            String code,
            String phone,
            String licensePlate,
            String model,
            LocalDate date,
            TimeSlot slot,
            BookingStatus status,
            PaymentStatus paymentStatus,
            List<ServiceCatalog> services,
            String notes
    ) {}

    private record DemoStaffSeed(
            String username,
            String email,
            String phone,
            String fullName,
            String password,
            String roleName,
            StaffStatus accountStatus,
            boolean requirePasswordChange
    ) {}
}
