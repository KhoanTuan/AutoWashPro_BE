package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.service.MailService;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.*;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAdminService {

    private static final String DEFAULT_PASSWORD = "Customer@123";

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final CustomerMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityTokenService securityTokenService;
    private final MailService mailService;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> listCustomers(String status, String keyword, int page, int size) {
        CustomerStatus customerStatus = parseStatus(status);
        Page<Customer> result = customerRepository.search(
                customerStatus,
                keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<CustomerResponse> content = result.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        return PageResponse.<CustomerResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerOptionResponse> listOptions() {
        return customerRepository.findAll(Sort.by("fullName").ascending()).stream()
                .filter(c -> c.getStatus() == CustomerStatus.ACTIVE)
                .map(mapper::toOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Customer customer = findCustomer(id);
        return mapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryStatsResponse getSummaryStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime churnThreshold = LocalDateTime.now().minusDays(60);

        long total = customerRepository.count();
        long active = customerRepository.countByStatus(CustomerStatus.ACTIVE);
        long vip = customerRepository.findAll().stream()
                .filter(c -> c.getTier() != null && !"REGULAR".equals(c.getTier().getTierName()))
                .count();

        return CustomerSummaryStatsResponse.builder()
                .totalCustomers(total)
                .activeCustomers(active)
                .vipMembers(vip)
                .newRegistrationsLast30Days(customerRepository.countRegisteredSince(thirtyDaysAgo))
                .churnRiskCount(customerRepository.countChurnRisk(CustomerStatus.ACTIVE, churnThreshold))
                .build();
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String phone = normalizePhone(request.getPhoneNumber());
        if (customerRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String plate = normalizePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
            throw new BadRequestException("License plate already exists");
        }

        LoyaltyTier tier = loyaltyTierRepository.findByTierName("MEMBER")
                .or(() -> loyaltyTierRepository.findByTierName("REGULAR"))
                .orElseThrow(() -> new ResourceNotFoundException("Default loyalty tier not found"));

        CustomerStatus status = request.getStatus() != null && !request.getStatus().isBlank()
                ? CustomerStatus.valueOf(request.getStatus().toUpperCase())
                : CustomerStatus.ACTIVE;

        Customer customer = Customer.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(phone)
                .email(blankToNull(request.getEmail()))
                .authProvider(CustomerAuthProvider.PHONE)
                .status(status)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .tier(tier)
                .build();
        customerRepository.save(customer);

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .licensePlate(plate)
                .model(request.getModel() != null ? request.getModel() : "Xe máy")
                .build();
        vehicleRepository.save(vehicle);

        return mapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);
        String phone = normalizePhone(request.getPhoneNumber());

        if (!customer.getPhoneNumber().equals(phone) && customerRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && (customer.getEmail() == null || !customer.getEmail().equalsIgnoreCase(request.getEmail()))
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        customer.setFullName(request.getFullName().trim());
        customer.setPhoneNumber(phone);
        customer.setEmail(blankToNull(request.getEmail()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            customer.setStatus(CustomerStatus.valueOf(request.getStatus().toUpperCase()));
        }

        String plate = normalizePlate(request.getLicensePlate());
        Vehicle primary = vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(id).orElse(null);
        if (primary != null) {
            if (!primary.getLicensePlate().equalsIgnoreCase(plate)
                    && vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
                throw new BadRequestException("License plate already exists");
            }
            primary.setLicensePlate(plate);
            if (request.getModel() != null) primary.setModel(request.getModel());
            vehicleRepository.save(primary);
        } else {
            if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
                throw new BadRequestException("License plate already exists");
            }
            vehicleRepository.save(Vehicle.builder()
                    .customer(customer)
                    .licensePlate(plate)
                    .model(request.getModel() != null ? request.getModel() : "Xe máy")
                    .build());
        }

        customerRepository.save(customer);
        return mapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, String status) {
        Customer customer = findCustomer(id);
        CustomerStatus targetStatus = CustomerStatus.valueOf(status.toUpperCase());
        CustomerStatus oldStatus = customer.getStatus();

        if (oldStatus == targetStatus) {
            return mapper.toResponse(customer);
        }

        customer.setStatus(targetStatus);
        customerRepository.save(customer);

        if (targetStatus == CustomerStatus.INACTIVE) {
            // Nghiệp vụ: Tự động hủy toàn bộ đơn đặt lịch chưa hoàn thành (PENDING, CONFIRMED, IN_PROGRESS) của khách
            List<Booking> customerBookings = bookingRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(id);
            for (Booking booking : customerBookings) {
                if (booking.getStatus() == BookingStatus.PENDING || 
                    booking.getStatus() == BookingStatus.CONFIRMED || 
                    booking.getStatus() == BookingStatus.IN_PROGRESS) {
                    
                    booking.setStatus(BookingStatus.CANCELLED_BY_CUSTOMER);
                    
                    // Hoàn trả voucher nếu có
                    if (booking.getVoucherCode() != null) {
                        try {
                            customerPromotionRepository.findByCustomerCustomerIdAndVoucherCode(id, booking.getVoucherCode())
                                .ifPresent(cp -> {
                                    cp.setStatus(CustomerPromotionStatus.ISSUED);
                                    customerPromotionRepository.save(cp);
                                    
                                    Promotion promotion = cp.getPromotion();
                                    if (promotion != null && promotion.getRedeemedCount() != null && promotion.getRedeemedCount() > 0) {
                                        promotion.setRedeemedCount(promotion.getRedeemedCount() - 1);
                                    }
                                });
                        } catch (Exception e) {
                            log.warn("Failed to restore voucher for booking {}: {}", booking.getBookingCode(), e.getMessage());
                        }
                    }
                    
                    bookingRepository.save(booking);
                    
                    // Phát sự kiện hủy lịch để giải phóng slot công suất và cập nhật thời gian thực
                    try {
                        eventPublisher.publishEvent(new BookingEvent(this, booking, BookingEventAction.CANCELLED,
                                "Tài khoản bị khóa",
                                "Lịch hẹn " + booking.getBookingCode() + " đã bị hủy do tài khoản bị khóa."));
                    } catch (Exception e) {
                        log.warn("Failed to publish booking cancel event: {}", e.getMessage());
                    }
                }
            }

            // Gửi thông báo hệ thống đến cho Khách hàng
            try {
                realtimeNotificationService.notifyGeneral(id, 
                        "🚨 Tài khoản đã bị khóa", 
                        "Tài khoản của bạn đã bị khóa bởi quản trị viên. Lịch hẹn chưa thực hiện đã được tự động hủy.", 
                        NotificationType.SYSTEM_ALERT);
            } catch (Exception e) {
                log.warn("Failed to send account lock notification: {}", e.getMessage());
            }
            log.info("Locked customer account id={} name={}. Cancelled active bookings and sent alert.", id, customer.getFullName());

        } else if (targetStatus == CustomerStatus.ACTIVE) {
            try {
                realtimeNotificationService.notifyGeneral(id, 
                        "🎉 Khôi phục tài khoản", 
                        "Tài khoản của bạn đã được quản trị viên khôi phục hoạt động.", 
                        NotificationType.SYSTEM_ALERT);
            } catch (Exception e) {
                log.warn("Failed to send reactivate notification: {}", e.getMessage());
            }
            log.info("Reactivated customer account id={} name={}. Sent welcome notification.", id, customer.getFullName());
        }

        return mapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Vehicle> findVehicleByPlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            return java.util.Optional.empty();
        }
        return vehicleRepository.findByLicensePlateIgnoreCase(normalizePlate(licensePlate));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Customer> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return java.util.Optional.empty();
        }
        return customerRepository.findByPhoneNumber(normalizePhone(phone));
    }

    @Transactional(readOnly = true)
    public Vehicle findPrimaryVehicle(Long customerId) {
        return vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(customerId).orElse(null);
    }

    /**
     * Resolve khách cho booking Nhánh B/C (admin/manager đặt cho khách).
     * Thứ tự ưu tiên: customerId → biển số (plate-first) → SĐT → tạo mới.
     * Ràng buộc: khách INACTIVE bị chặn; khách mới bắt buộc có SĐT; không tạo trùng khách.
     * Khách mới được tạo ở trạng thái PENDING_ACTIVATION + mật khẩu không dùng được;
     * nếu có email → lên lịch gửi link claim (đặt mật khẩu + kích hoạt) sau khi commit.
     * Lưu ý: hàm này CHỈ resolve khách — việc gắn/ tạo xe do {@link #resolveVehicle} đảm nhiệm.
     */
    @Transactional
    public ResolvedCustomer resolveOrCreateForBooking(Long customerId, String fullName, String phone, String email,
                                                      String licensePlate, String model) {
        String plate = (licensePlate != null && !licensePlate.isBlank()) ? normalizePlate(licensePlate) : null;
        String normalizedPhone = (phone != null && !phone.isBlank()) ? normalizePhone(phone) : null;

        // 1) Chọn khách rõ ràng theo ID
        if (customerId != null) {
            Customer existing = findCustomer(customerId);
            ensureBookable(existing);
            return new ResolvedCustomer(existing, false, false);
        }

        // 2) Plate-first: biển số đã tồn tại → dùng đúng chủ xe (tránh tạo trùng khách)
        if (plate != null) {
            Customer owner = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                    .map(Vehicle::getCustomer)
                    .orElse(null);
            if (owner != null) {
                ensureBookable(owner);
                applyOptionalInfo(owner, fullName, email);
                return new ResolvedCustomer(customerRepository.save(owner), false, false);
            }
        }

        // 3) Khách mới bắt buộc có SĐT (để chạy loyalty/retention)
        if (normalizedPhone == null) {
            throw new BadRequestException("Phone number is required to book for a new walk-in customer");
        }

        // 4) Tra theo SĐT — đã có thì dùng lại
        Customer byPhone = customerRepository.findByPhoneNumber(normalizedPhone).orElse(null);
        if (byPhone != null) {
            ensureBookable(byPhone);
            applyOptionalInfo(byPhone, fullName, email);
            return new ResolvedCustomer(customerRepository.save(byPhone), false, false);
        }

        // 5) Tạo khách mới (SĐT chưa tồn tại, biển số chưa tồn tại)
        String cleanEmail = blankToNull(email);
        if (cleanEmail != null && customerRepository.existsByEmail(cleanEmail)) {
            throw new BadRequestException("Email already exists");
        }

        LoyaltyTier tier = loyaltyTierRepository.findByTierName("MEMBER")
                .or(() -> loyaltyTierRepository.findByTierName("REGULAR"))
                .orElseThrow(() -> new ResourceNotFoundException("Default loyalty tier not found"));

        Customer customer = Customer.builder()
                .fullName(fullName != null && !fullName.isBlank() ? fullName.trim() : "Walk-in Guest")
                .phoneNumber(normalizedPhone)
                .email(cleanEmail)
                .authProvider(cleanEmail != null ? CustomerAuthProvider.EMAIL : CustomerAuthProvider.PHONE)
                .status(CustomerStatus.PENDING_ACTIVATION)
                .passwordHash(unusablePassword())
                .tier(tier)
                .build();
        customerRepository.save(customer);

        boolean inviteSent = scheduleClaimInvite(customer);
        return new ResolvedCustomer(customer, true, inviteSent);
    }

    private void ensureBookable(Customer customer) {
        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BadRequestException(
                    "Customer is INACTIVE and cannot book. Reactivate the account first.");
        }
    }

    private void applyOptionalInfo(Customer customer, String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            customer.setFullName(fullName.trim());
        }
        // Chỉ điền email khi hồ sơ chưa có và email chưa bị khách khác dùng (tránh đụng unique).
        String cleanEmail = blankToNull(email);
        if (cleanEmail != null
                && (customer.getEmail() == null || customer.getEmail().isBlank())
                && !customerRepository.existsByEmail(cleanEmail)) {
            customer.setEmail(cleanEmail);
        }
    }

    /**
     * Tạo token claim + lên lịch gửi email kích hoạt sau khi transaction commit
     * (mail fail không làm rollback booking). Trả về true nếu đã lên lịch gửi.
     */
    private boolean scheduleClaimInvite(Customer customer) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return false;
        }
        SecurityToken token = securityTokenService.createToken(customer, SecurityTokenType.ACCOUNT_CLAIM);
        final String email = customer.getEmail();
        final String name = customer.getFullName();
        final String tokenValue = token.getToken();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendClaimInvite(email, name, tokenValue);
                }
            });
        } else {
            sendClaimInvite(email, name, tokenValue);
        }
        return true;
    }

    private void sendClaimInvite(String email, String name, String tokenValue) {
        try {
            MailService.SendResult result = mailService.sendAccountClaimEmail(email, name, tokenValue);
            log.info("[Booking/Claim] invite to {} | mode={} | success={}",
                    email, result.mode(), result.success());
        } catch (Exception ex) {
            log.error("[Booking/Claim] failed to send invite to {}: {}", email, ex.getMessage(), ex);
        }
    }

    private String unusablePassword() {
        return passwordEncoder.encode("!" + UUID.randomUUID());
    }

    @Transactional
    public Vehicle resolveVehicle(Customer customer, String licensePlate, String model) {
        String plate = normalizePlate(licensePlate);
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate).orElse(null);
        if (vehicle != null) {
            if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new BadRequestException("License plate belongs to another customer");
            }
            if (model != null && !model.isBlank()) {
                vehicle.setModel(model);
            }
            return vehicleRepository.save(vehicle);
        }
        return vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .licensePlate(plate)
                .model(model != null ? model : "Xe máy")
                .build());
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private CustomerStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return CustomerStatus.valueOf(status.toUpperCase());
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("\\s+", "");
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
