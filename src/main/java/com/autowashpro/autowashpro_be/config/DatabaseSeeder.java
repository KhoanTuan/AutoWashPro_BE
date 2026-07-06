package com.autowashpro.autowashpro_be.config;


import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceCatalogRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.notification.entity.*;
import com.autowashpro.autowashpro_be.modules.notification.repository.NotificationRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerAuthProvider;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
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
    private final NotificationRepository notificationRepository;

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
                    "Cashier@123", "ROLE_CASHIER", StaffStatus.ACTIVE, false)
    );

    @Override
    @Transactional
    public void run(String... args) {
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
        seedDemoNotifications();
        log.info("Demo staff ready — admin/Admin@123, manager/Manager@123, cashier/Cashier@123");
        log.info("Demo customers ready — password Customer@123");
        log.info("Demo service catalog, time slots, bookings & notifications seeded successfully for E2E-1!");
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

    private void seedDemoCustomers() {
        Map<String, LoyaltyTier> tiers = new HashMap<>();
        loyaltyTierRepository.findAll().forEach(t -> tiers.put(t.getTierName(), t));

        List<DemoCustomerSeed> seeds = List.of(
                new DemoCustomerSeed("annguyen", "Nguyen Van An", "0902000001", "an.nguyen@email.com", "REGULAR", CustomerStatus.ACTIVE, 12, 2400000, 850),
                new DemoCustomerSeed("binhtran", "Tran Thi Binh", "0902000002", "binh.tran@email.com", "SILVER", CustomerStatus.ACTIVE, 28, 5200000, 2100),
                new DemoCustomerSeed("cuongle", "Le Minh Cuong", "0902000003", "cuong.le@email.com", "GOLD", CustomerStatus.ACTIVE, 45, 9800000, 4800),
                new DemoCustomerSeed("dungpham", "Pham Thi Dung", "0902000004", "dung.pham@email.com", "PLATINUM", CustomerStatus.ACTIVE, 62, 15200000, 9200),
                new DemoCustomerSeed("emhoang", "Hoang Van Em", "0902000005", "em.hoang@email.com", "REGULAR", CustomerStatus.ACTIVE, 5, 650000, 120),
                new DemoCustomerSeed("phuongvo", "Vo Thi Phuong", "0902000006", "phuong.vo@email.com", "SILVER", CustomerStatus.INACTIVE, 18, 3100000, 900),
                new DemoCustomerSeed("giangdang", "Dang Quoc Giang", "0902000007", "giang.dang@email.com", "REGULAR", CustomerStatus.ACTIVE, 2, 180000, 40),
                new DemoCustomerSeed("hoabui", "Bui Thi Hoa", "0902000008", "hoa.bui@email.com", "GOLD", CustomerStatus.ACTIVE, 35, 7200000, 3500)
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
                .loyaltyPoints(seed.points())
                .lastCompletedBookingAt(seed.visits() > 0 ? LocalDateTime.now().minusDays(3) : null)
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
        customer.setLoyaltyPoints(seed.points());
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
            new DemoVehicleSeed("0902000008", "61H-55667", "Honda Lead")
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
                TimeSlot.builder().startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).maxCapacity(3).isActive(true).displayOrder(1).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).maxCapacity(3).isActive(true).displayOrder(2).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).maxCapacity(3).isActive(true).displayOrder(3).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 0)).maxCapacity(3).isActive(true).displayOrder(4).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(16, 0)).maxCapacity(3).isActive(true).displayOrder(5).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 0)).maxCapacity(3).isActive(true).displayOrder(6).dayOfWeek("ALL").build(),
                TimeSlot.builder().startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(18, 0)).maxCapacity(5).isActive(true).displayOrder(7).dayOfWeek("WEEKEND").build(),
                TimeSlot.builder().startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0)).maxCapacity(5).isActive(true).displayOrder(8).dayOfWeek("WEEKEND").build()
        );
        timeSlotRepository.saveAll(slots);
    }


    private void seedDemoBookings() {
        if (bookingRepository.count() > 0) return;

        List<ServiceCatalog> allServices = serviceCatalogRepository.findAll();
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        if (allServices.isEmpty() || allSlots.isEmpty()) return;

        ServiceCatalog pkgStd = allServices.stream().filter(s -> "PKG-STD".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog pkgDeluxe = allServices.stream().filter(s -> "PKG-DELUXE".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog pkgUltimate = allServices.stream().filter(s -> "PKG-ULTIMATE".equals(s.getServiceCode())).findFirst().orElse(allServices.get(0));
        ServiceCatalog addHelmet = allServices.stream().filter(s -> "ADD-HELMET".equals(s.getServiceCode())).findFirst().orElse(null);

        TimeSlot slot8_9 = allSlots.stream().filter(s -> s.getDisplayOrder() == 1).findFirst().orElse(allSlots.get(0));
        TimeSlot slot9_10 = allSlots.stream().filter(s -> s.getDisplayOrder() == 2).findFirst().orElse(allSlots.get(0));
        TimeSlot slot10_11 = allSlots.stream().filter(s -> s.getDisplayOrder() == 3).findFirst().orElse(allSlots.get(0));
        TimeSlot slot14_15 = allSlots.stream().filter(s -> s.getDisplayOrder() == 4).findFirst().orElse(allSlots.get(0));

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Booking 1: Customer 0902000001 (Tran Van An), Slot 8-9 today, PKG-STD + ADD-HELMET, PENDING
        customerRepository.findByPhoneNumber("0902000001").ifPresent(cus -> {
            Booking b1 = Booking.builder()
                    .bookingCode("NV-1001")
                    .customer(cus)
                    .licensePlate("29A-12345")
                    .model("Honda SH 150i")
                    .bookingDate(today)
                    .timeSlot(slot8_9)
                    .status(BookingStatus.PENDING)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .totalEstimatedAmount(pkgStd.getPrice().add(addHelmet != null ? addHelmet.getPrice() : BigDecimal.ZERO))
                    .notes("Khách dặn rửa kỹ lốp xe")
                    .build();
            b1.addItem(createBookingItem(pkgStd));
            if (addHelmet != null) b1.addItem(createBookingItem(addHelmet));
            bookingRepository.save(b1);
        });

        // Booking 2: Customer 0902000002 (Le Thi Mai), Slot 9-10 today, PKG-DELUXE, CONFIRMED
        customerRepository.findByPhoneNumber("0902000002").ifPresent(cus -> {
            Booking b2 = Booking.builder()
                    .bookingCode("NV-1002")
                    .customer(cus)
                    .licensePlate("51B-67890")
                    .model("Yamaha Grande")
                    .bookingDate(today)
                    .timeSlot(slot9_10)
                    .status(BookingStatus.CONFIRMED)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .totalEstimatedAmount(pkgDeluxe.getPrice())
                    .notes("Khách hẹn đến đúng giờ")
                    .build();
            b2.addItem(createBookingItem(pkgDeluxe));
            bookingRepository.save(b2);
        });

        // Booking 3: Customer 0902000003 (Pham Huu Tho), Slot 10-11 today, PKG-ULTIMATE, IN_PROGRESS
        customerRepository.findByPhoneNumber("0902000003").ifPresent(cus -> {
            Booking b3 = Booking.builder()
                    .bookingCode("NV-1003")
                    .customer(cus)
                    .licensePlate("43C-11223")
                    .model("Vespa Sprint")
                    .bookingDate(today)
                    .timeSlot(slot10_11)
                    .status(BookingStatus.IN_PROGRESS)
                    .paymentStatus(PaymentStatus.PAID)
                    .totalEstimatedAmount(pkgUltimate.getPrice())
                    .notes("Đang rửa chi tiết")
                    .build();
            b3.addItem(createBookingItem(pkgUltimate));
            bookingRepository.save(b3);
        });

        // Booking 4: Customer 0902000004 (Nguyen Hoang Yen), Slot 14-15 tomorrow, PKG-DELUXE, CANCELLED_BY_CUSTOMER
        customerRepository.findByPhoneNumber("0902000004").ifPresent(cus -> {
            Booking b4 = Booking.builder()
                    .bookingCode("NV-1004")
                    .customer(cus)
                    .licensePlate("30D-44556")
                    .model("Honda Vision")
                    .bookingDate(tomorrow)
                    .timeSlot(slot14_15)
                    .status(BookingStatus.CANCELLED_BY_CUSTOMER)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .totalEstimatedAmount(pkgDeluxe.getPrice())
                    .notes("Khách bận đột xuất nên hủy")
                    .build();
            b4.addItem(createBookingItem(pkgDeluxe));
            bookingRepository.save(b4);
        });
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
            boolean requirePasswordChange
    ) {}
}
