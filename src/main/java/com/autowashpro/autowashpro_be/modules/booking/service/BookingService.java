package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingItemResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotAvailabilityResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotLockResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotOccupancyResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.GarageClosureRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceCatalogRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction;
import com.autowashpro.autowashpro_be.modules.booking.event.SlotCapacityChangeEvent;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionSource;
import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final GarageClosureRepository garageClosureRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final com.autowashpro.autowashpro_be.modules.booking.repository.SlotLockRepository slotLockRepository;
    private final com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository customerRepository;
    private final com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository loyaltyTierRepository;
    private final com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyConfigRepository loyaltyConfigRepository;
    private final com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository pointTransactionRepository;

    private static final List<BookingStatus> ACTIVE_CAPACITY_STATUSES = Arrays.asList(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED
    );

    private static final List<BookingStatus> VEHICLE_OCCUPIED_STATUSES = Arrays.asList(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED
    );

    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> getAvailableSlots(LocalDate date, Customer customer) {
        validateBookingDate(date, customer);

        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(date);
        boolean isClosedHoliday = closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay());
        String closureReasonText = isClosedHoliday ? closureOpt.get().getReason() : null;
        String holidayReason = isClosedHoliday ? ("CLOSED_HOLIDAY: " + closureReasonText) : null;

        List<TimeSlot> slots = timeSlotRepository.findAllByOrderByDisplayOrderAscStartTimeAsc();
        List<SlotAvailabilityResponse> responses = new ArrayList<>();

        for (TimeSlot slot : slots) {
            // Check if slot applies to this day of week
            if (!isSlotApplicableForDate(slot, date)) {
                continue;
            }

            int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slot.getSlotId())
                    .map(SlotLock::getLockCount)
                    .orElse(0);
            int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                    date, slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);
            int availableCapacity = Math.max(0, slot.getMaxCapacity() - bookedCount - lockedCount);

            boolean isPast = date.isEqual(LocalDate.now()) && slot.getStartTime().isBefore(LocalTime.now());
            boolean isFull = availableCapacity <= 0 || (slot.getMaxCapacity() > 0 && bookedCount >= slot.getMaxCapacity()) || lockedCount > 0;
            boolean isInactive = !slot.getIsActive();

            boolean isAvailable = !isClosedHoliday && !isInactive && !isPast && !isFull;
            String reason = null;
            if (isClosedHoliday) {
                reason = holidayReason;
            } else if (isInactive) {
                reason = "MAINTENANCE";
            } else if (isPast) {
                reason = "PAST";
            } else if (isFull) {
                reason = "FULL";
            }

            responses.add(SlotAvailabilityResponse.builder()
                    .slotId(slot.getSlotId())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .maxCapacity(slot.getMaxCapacity())
                    .bookedCount(bookedCount)
                    .availableCapacity(availableCapacity)
                    .isAvailable(isAvailable)
                    .disabledReason(reason)
                    .isDayLocked(isClosedHoliday)
                    .closureReason(closureReasonText)
                    .build());
        }

        return responses;
    }

    private boolean isSlotApplicableForDate(TimeSlot slot, LocalDate date) {
        String dowConfig = slot.getDayOfWeek();
        if (dowConfig == null || dowConfig.trim().isEmpty() || dowConfig.equalsIgnoreCase("ALL")) {
            return true;
        }
        int val = date.getDayOfWeek().getValue(); // 1 = MON, ..., 7 = SUN
        boolean isWeekend = (val == 6 || val == 7);
        if (dowConfig.equalsIgnoreCase("WEEKDAY") && !isWeekend) {
            return true;
        }
        if (dowConfig.equalsIgnoreCase("WEEKEND") && isWeekend) {
            return true;
        }
        String fullDayName = date.getDayOfWeek().name(); // MONDAY, TUESDAY...
        String shortName = fullDayName.substring(0, 3); // MON, TUE...
        return dowConfig.equalsIgnoreCase(shortName) || dowConfig.equalsIgnoreCase(fullDayName);
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản của bạn hiện đang bị tạm khóa hoặc chưa kích hoạt. Vui lòng liên hệ Hotline xưởng để được hỗ trợ!");
        }

        List<BookingStatus> checkStatuses = Arrays.asList(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS,
                BookingStatus.COMPLETED
        );
        boolean hasDuplicate = bookingRepository.existsByCustomerCustomerIdAndBookingDateAndTimeSlotSlotIdAndStatusIn(
                customer.getCustomerId(), request.getBookingDate(), request.getTimeSlotId(), checkStatuses);
        if (hasDuplicate) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Khách hàng đã có lịch đặt hoặc đã thanh toán ở khung giờ này.");
        }

        validateBookingDate(request.getBookingDate(), customer);

        // Kiểm tra xem khách hàng có đang bị phạt khóa đặt lịch 7 ngày hay không (do trễ hẹn No-Show >= 3 lần trong vòng 30 ngày)
        java.time.LocalDateTime startOf30DaysAgo = java.time.LocalDateTime.now().minusDays(30);
        long noShowCount = bookingRepository.countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(
                customer.getCustomerId(), 
                Arrays.asList(BookingStatus.CANCELLED_NO_SHOW), 
                startOf30DaysAgo
        );
        if (noShowCount >= 3) {
            Optional<Booking> latestNoShowOpt = bookingRepository.findFirstByCustomerCustomerIdAndStatusOrderByUpdatedAtDesc(
                    customer.getCustomerId(), BookingStatus.CANCELLED_NO_SHOW);
            if (latestNoShowOpt.isPresent()) {
                java.time.LocalDateTime banUntil = latestNoShowOpt.get().getUpdatedAt().plusDays(7);
                if (java.time.LocalDateTime.now().isBefore(banUntil)) {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    throw new BadRequestException("Tài khoản của bạn đã bị tạm khóa tính năng đặt lịch online đến " + banUntil.format(formatter) + " do vi phạm trễ hẹn (No-Show) " + noShowCount + " lần trong vòng 30 ngày qua!");
                }
            }
        }

        // Chặn spam đặt/hủy liên tục trong ngày (tối đa 3 lần/ngày)
        java.time.LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long canceledToday = bookingRepository.countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(
                customer.getCustomerId(), 
                Arrays.asList(BookingStatus.CANCELLED_BY_CUSTOMER, BookingStatus.CANCELLED_NO_SHOW), 
                startOfToday
        );
        if (canceledToday >= 3) {
            throw new BadRequestException("Tài khoản của bạn đã hủy " + canceledToday + " đơn đặt lịch trong ngày hôm nay. Do vi phạm chế tài hủy từ 3 đơn trở lên (bị trừ 20% điểm penalty), tài khoản bị tạm cấm đặt lịch mới trong 1 ngày (cho đến ngày mai)!");
        }

        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(request.getBookingDate());
        if (closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay())) {
            throw new BadRequestException("Xưởng đóng cửa nghỉ lễ trong ngày " + request.getBookingDate() + ". Lý do: " + closureOpt.get().getReason());
        }

        TimeSlot slot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + request.getTimeSlotId()));

        if (!slot.getIsActive()) {
            throw new BadRequestException("Khung giờ này đang tạm ngưng phục vụ hoặc bảo trì");
        }

        if (!isSlotApplicableForDate(slot, request.getBookingDate())) {
            throw new BadRequestException("Khung giờ này không áp dụng cho thứ/ngày được chọn (" + request.getBookingDate().getDayOfWeek() + ")");
        }

        if (request.getBookingDate().isEqual(LocalDate.now()) && slot.getStartTime().isBefore(LocalTime.now())) {
            throw new BadRequestException("Khung giờ này đã qua trong ngày hôm nay");
        }

        int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(request.getBookingDate(), slot.getSlotId())
                .map(SlotLock::getLockCount)
                .orElse(0);
        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                request.getBookingDate(), slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);
        if (bookedCount + lockedCount >= slot.getMaxCapacity()) {
            throw new BadRequestException("Khung giờ này đã đầy xe (Đã đặt: " + bookedCount + ", Đã khóa: " + lockedCount + "/" + slot.getMaxCapacity() + "), vui lòng chọn khung giờ khác!");
        }

        int maxDailyBookings = (customer != null && customer.getTier() != null && customer.getTier().getTierName() != null) ? switch (customer.getTier().getTierName().toUpperCase()) {
            case "SILVER" -> 5;
            case "GOLD" -> 10;
            case "PLATINUM" -> 20;
            default -> 3;
        } : 3;
        int customerTodayBookings = bookingRepository.countByCustomerCustomerIdAndBookingDateAndStatusIn(
                customer.getCustomerId(), request.getBookingDate(), ACTIVE_CAPACITY_STATUSES);
        if (customerTodayBookings >= maxDailyBookings) {
            String tierName = (customer != null && customer.getTier() != null && customer.getTier().getTierName() != null) ? customer.getTier().getTierName() : "REGULAR";
            throw new BadRequestException("Tài khoản của bạn đang có " + customerTodayBookings + "/" + maxDailyBookings + " đơn đặt lịch đang giữ chỗ/chưa hoàn thành trong ngày " + 
                    request.getBookingDate() + " (giới hạn theo hạng " + tierName + "). Vui lòng hoàn thành dịch vụ và thanh toán (hoặc hủy lịch cũ) trước khi đặt thêm!");
        }


        String cleanPlate = request.getLicensePlate() != null ? request.getLicensePlate().trim().toUpperCase() : "";
        if (cleanPlate.length() < 5 || cleanPlate.length() > 20) {
            throw new BadRequestException("Biển số xe phải từ 5 đến 20 ký tự (ví dụ: 29-H1 555.55)");
        }
        Optional<Vehicle> existingVehOpt = vehicleRepository.findByLicensePlateIgnoreCase(cleanPlate);
        if (existingVehOpt.isEmpty() || !existingVehOpt.get().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BadRequestException("Biển số xe '" + cleanPlate + "' chưa có trong danh sách xe (Garage) của bạn! Vui lòng thêm xe vào Garage trước khi đặt lịch rửa.");
        }
        Vehicle vehicle = existingVehOpt.get();

        ServiceCatalog packageService = null;
        if (request.getPackageId() != null && request.getPackageId() > 0) {
            packageService = serviceCatalogRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gói rửa xe được chọn không hợp lệ với ID: " + request.getPackageId()));
            if (!packageService.getIsActive() || packageService.getServiceType() != ServiceType.PACKAGE) {
                throw new BadRequestException("Gói rửa xe được chọn không hợp lệ hoặc đã ngừng kinh doanh");
            }
        } else {
            if (request.getAddonIds() == null || request.getAddonIds().isEmpty()) {
                throw new BadRequestException("Vui lòng chọn 1 gói dịch vụ dọn rửa chính hoặc chọn ít nhất 1 tiện ích add-on!");
            }
        }

        // Tính tổng số phút của đơn đặt mới = Gói chính + các dịch vụ chọn thêm
        int newBookingDurationMinutes = (packageService != null && packageService.getDurationMinutes() != null) ? packageService.getDurationMinutes() : 0;
        if (request.getAddonIds() != null && !request.getAddonIds().isEmpty()) {
            List<ServiceCatalog> addons = serviceCatalogRepository.findAllById(request.getAddonIds());
            for (ServiceCatalog addon : addons) {
                if (addon.getIsActive() && addon.getServiceType() == ServiceType.ADDON) {
                    newBookingDurationMinutes += addon.getDurationMinutes() != null ? addon.getDurationMinutes() : 0;
                }
            }
        }
        newBookingDurationMinutes = Math.max(15, newBookingDurationMinutes);

        LocalDateTime newStartDateTime = request.getBookingDate().atTime(slot.getStartTime());
        LocalDateTime newEndDateTime = newStartDateTime.plusMinutes(newBookingDurationMinutes);

        // Kiểm tra xem xe này đã có lịch hẹn nào trùng khoảng thời gian [newStartDateTime, newEndDateTime) hay không
        List<Booking> existingVehicleBookings = bookingRepository.findAllByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(
                request.getBookingDate(), vehicle.getLicensePlate(), VEHICLE_OCCUPIED_STATUSES);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Booking existingBooking : existingVehicleBookings) {
            if (existingBooking.getTimeSlot() == null) continue;
            LocalDateTime existingStartDateTime = existingBooking.getBookingDate().atTime(existingBooking.getTimeSlot().getStartTime());
            int existingDurationMinutes = calculateBookingDurationMinutes(existingBooking);
            LocalDateTime existingEndDateTime = existingStartDateTime.plusMinutes(existingDurationMinutes);

            // Giao cắt thời gian (Overlap) khi: newStart < existingEnd AND existingStart < newEnd
            boolean isOverlapping = newStartDateTime.isBefore(existingEndDateTime) && existingStartDateTime.isBefore(newEndDateTime);

            if (isOverlapping) {
                throw new BadRequestException(String.format(
                        "Xe mang biển số '%s' đã có lịch hẹn từ %s đến %s (tổng thời gian dịch vụ %d phút). Vui lòng chọn khung giờ khác sau %s cho xe!",
                        vehicle.getLicensePlate(),
                        existingStartDateTime.format(timeFormatter),
                        existingEndDateTime.format(timeFormatter),
                        existingDurationMinutes,
                        existingEndDateTime.format(timeFormatter)
                ));
            }
        }

        Booking booking = Booking.builder()
                .bookingCode(generateUniqueBookingCode(request.getBookingDate()))
                .customer(customer)
                .licensePlate(vehicle.getLicensePlate())
                .model(vehicle.getModel() != null ? vehicle.getModel() : (request.getModel() != null ? request.getModel().trim() : "Xe máy"))

                .bookingDate(request.getBookingDate())
                .timeSlot(slot)
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (packageService != null) {
            totalAmount = packageService.getPrice() != null ? packageService.getPrice() : BigDecimal.ZERO;
            booking.addItem(BookingItem.builder()
                    .serviceId(packageService.getServiceId())
                    .serviceCodeSnapshot(packageService.getServiceCode())
                    .serviceNameSnapshot(packageService.getServiceName())
                    .serviceTypeSnapshot(ServiceType.PACKAGE)
                    .priceSnapshot(packageService.getPrice())
                    .build());
        } else {
            booking.addItem(BookingItem.builder()
                    .serviceId(0L)
                    .serviceCodeSnapshot("CUSTOM_PACKAGE")
                    .serviceNameSnapshot("Gói custom")
                    .serviceTypeSnapshot(ServiceType.PACKAGE)
                    .priceSnapshot(BigDecimal.ZERO)
                    .build());
        }

        if (request.getAddonIds() != null && !request.getAddonIds().isEmpty()) {
            Set<Long> uniqueAddonIds = new HashSet<>(request.getAddonIds());
            for (Long addonId : uniqueAddonIds) {
                ServiceCatalog addonService = serviceCatalogRepository.findById(addonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Addon service not found with id: " + addonId));
                if (!addonService.getIsActive() || addonService.getServiceType() != ServiceType.ADDON) {
                    throw new BadRequestException("Dịch vụ thêm không hợp lệ hoặc đã ngừng kinh doanh: " + addonId);
                }
                totalAmount = totalAmount.add(addonService.getPrice());
                booking.addItem(BookingItem.builder()
                        .serviceId(addonService.getServiceId())
                        .serviceCodeSnapshot(addonService.getServiceCode())
                        .serviceNameSnapshot(addonService.getServiceName())
                        .serviceTypeSnapshot(ServiceType.ADDON)
                        .priceSnapshot(addonService.getPrice())
                        .build());
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String code = request.getVoucherCode().trim();
            CustomerPromotion customerPromotion = customerPromotionRepository.findByCustomerCustomerIdAndVoucherCode(
                    customer.getCustomerId(), code)
                    .orElseThrow(() -> new BadRequestException("Mã giảm giá '" + code + "' không tồn tại trong ví của bạn!"));

            if (customerPromotion.getStatus() != CustomerPromotionStatus.ISSUED) {
                throw new BadRequestException("Mã giảm giá '" + code + "' đã được sử dụng hoặc đã hết hạn!");
            }

            Promotion promotion = customerPromotion.getPromotion();
            if (promotion.getStatus() != PromotionStatus.ACTIVE) {
                throw new BadRequestException("Chiến dịch khuyến mãi cho mã này đã kết thúc hoặc tạm ngưng!");
            }

            if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Mã giảm giá '" + code + "' đã quá hạn sử dụng!");
            }

            // 1. Ràng buộc gói dịch vụ (applicableServiceCode)
            if (promotion.getApplicableServiceCode() != null && !promotion.getApplicableServiceCode().trim().isEmpty()) {
                String reqServiceCode = packageService.getServiceCode();
                if (!promotion.getApplicableServiceCode().trim().equalsIgnoreCase(reqServiceCode)) {
                    throw new BadRequestException("Mã ưu đãi này chỉ áp dụng cho gói dịch vụ: " + promotion.getApplicableServiceCode());
                }
            }

            // 2. Ràng buộc ngày trong tuần (applicableDays)
            if (promotion.getApplicableDays() != null && !promotion.getApplicableDays().trim().isEmpty()) {
                String bookingDayOfWeek = request.getBookingDate().getDayOfWeek().name().substring(0, 3).toUpperCase();
                String appDays = promotion.getApplicableDays().toUpperCase();
                if (!appDays.contains(bookingDayOfWeek)) {
                    throw new BadRequestException("Mã ưu đãi này chỉ áp dụng cho các ngày: " + promotion.getApplicableDays());
                }
            }

            // 3. Ràng buộc giá trị đơn hàng tối thiểu (minOrderValue)
            // Chỉ áp dụng cho các voucher tiếp thị/phát tặng miễn phí (costPoints == 0),
            // bỏ qua cho các voucher đổi bằng điểm Loyalty (đã tự trả giá bằng điểm).
            if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                boolean isPointsExchange = (promotion.getCostPoints() != null && promotion.getCostPoints() > 0)
                        || (customerPromotion.getSource() == CustomerPromotionSource.EXCHANGE);
                if (!isPointsExchange && totalAmount.compareTo(promotion.getMinOrderValue()) < 0) {
                    throw new BadRequestException("Mã ưu đãi này chỉ áp dụng cho đơn hàng từ " + 
                            promotion.getMinOrderValue().setScale(0, java.math.RoundingMode.HALF_UP).toString() + " đ trở lên!");
                }
            }

            // Calculate discount
            if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                discountAmount = promotion.getValue() != null ? promotion.getValue() : BigDecimal.ZERO;
            } else if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
                BigDecimal pct = promotion.getValue() != null ? promotion.getValue() : BigDecimal.ZERO;
                discountAmount = totalAmount.multiply(pct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else if (promotion.getDiscountType() == DiscountType.FREE_SERVICE) {
                discountAmount = totalAmount;
            }

            // Áp dụng trần giảm tối đa (maxDiscountAmount) nếu được cấu hình
            if (promotion.getMaxDiscountAmount() != null && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discountAmount = promotion.getMaxDiscountAmount();
                }
            }

            if (discountAmount.compareTo(totalAmount) > 0) {
                discountAmount = totalAmount;
            }

            booking.setVoucherCode(code);
            booking.setDiscountAmount(discountAmount);

            // Tạm thời đánh dấu đã dùng để khóa voucher tránh double claim
            customerPromotion.setStatus(CustomerPromotionStatus.USED);
            customerPromotionRepository.save(customerPromotion);

            // Tăng số lượng đã được sử dụng thực tế của chiến dịch
            promotion.setRedeemedCount((promotion.getRedeemedCount() != null ? promotion.getRedeemedCount() : 0) + 1);
        }

        booking.setTotalEstimatedAmount(totalAmount);
        booking.setFinalAmount(totalAmount.subtract(discountAmount));

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Created booking successfully: {} for license plate {}. Discount: {}, Final: {}", 
                savedBooking.getBookingCode(), savedBooking.getLicensePlate(), savedBooking.getDiscountAmount(), savedBooking.getFinalAmount());
        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CREATED));

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCustomerBookings(Customer customer) {
        List<Booking> bookings = bookingRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId());
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Customer customer) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if (!booking.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BadRequestException("Bạn không có quyền xem đơn đặt lịch này!");
        }
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBookingByCustomer(Long id, Customer customer) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if (!booking.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BadRequestException("Bạn không có quyền thao tác trên đơn đặt lịch này!");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Đơn đặt lịch đang ở trạng thái '" + booking.getStatus() + "', không thể hủy!");
        }

        // Khách hàng có thể hủy đơn bất kỳ lúc nào trước giờ hẹn
        LocalDateTime slotStartTime = LocalDateTime.of(booking.getBookingDate(), booking.getTimeSlot().getStartTime());
        if (LocalDateTime.now().isAfter(slotStartTime)) {
            throw new BadRequestException("Không thể hủy đơn đặt lịch đã qua giờ hẹn!");
        }

        // Đếm số lần hủy lịch của khách hàng trong ngày hôm nay (trước lần hủy hiện tại)
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long canceledToday = bookingRepository.countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(
                customer.getCustomerId(),
                Arrays.asList(BookingStatus.CANCELLED_BY_CUSTOMER, BookingStatus.CANCELLED_NO_SHOW),
                startOfToday
        );

        // Lấy thông tin khách hàng từ DB để đảm bảo số điểm mới nhất
        Customer dbCustomer = customerRepository.findById(customer.getCustomerId())
                .orElse(customer);

        int currentPoints = dbCustomer.getLoyaltyPoints() != null ? dbCustomer.getLoyaltyPoints() : 0;
        int penaltyPoints = 0;
        String penaltyMsg = "";

        if (canceledToday == 0) {
            // Lần 1: Tha (0 điểm penalty)
            penaltyPoints = 0;
            penaltyMsg = "Hủy lịch lần 1 trong ngày (Tha - Không trừ điểm penalty).";
        } else if (canceledToday == 1) {
            // Lần 2: Trừ 10 điểm penalty
            penaltyPoints = 10;
            int newPoints = Math.max(0, currentPoints - penaltyPoints);
            dbCustomer.setLoyaltyPoints(newPoints);
            customerRepository.save(dbCustomer);

            PointTransaction pt = PointTransaction.builder()
                    .customer(dbCustomer)
                    .points(-penaltyPoints)
                    .activityType(PointActivityType.PENALTY)
                    .bookingCode(booking.getBookingCode())
                    .build();
            pointTransactionRepository.save(pt);

            penaltyMsg = "Hủy lịch lần 2 trong ngày. Trừ 10 điểm penalty (Điểm còn lại: " + newPoints + " pts).";
        } else {
            // Lần 3 trở đi: Trừ 20% số điểm hiện có (tối thiểu 40pts, không giới hạn trần) & cấm đặt lịch 1 ngày
            int calculated20Pct = (int) Math.round(currentPoints * 0.05);
            penaltyPoints = Math.max(40, calculated20Pct);
            int newPoints = Math.max(0, currentPoints - penaltyPoints);
            dbCustomer.setLoyaltyPoints(newPoints);
            customerRepository.save(dbCustomer);

            PointTransaction pt = PointTransaction.builder()
                    .customer(dbCustomer)
                    .points(-penaltyPoints)
                    .activityType(PointActivityType.PENALTY)
                    .bookingCode(booking.getBookingCode())
                    .build();
            pointTransactionRepository.save(pt);

            penaltyMsg = "Hủy lịch lần " + (canceledToday + 1) + " trong ngày. Trừ 5% điểm penalty (" + penaltyPoints + " pts) và bị cấm đặt lịch 1 ngày!";
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_CUSTOMER);
        
        if (booking.getVoucherCode() != null) {
            customerPromotionRepository.findByCustomerCustomerIdAndVoucherCode(customer.getCustomerId(), booking.getVoucherCode())
                .ifPresent(cp -> {
                    cp.setStatus(CustomerPromotionStatus.ISSUED);
                    customerPromotionRepository.save(cp);
                    
                    Promotion promotion = cp.getPromotion();
                    if (promotion != null && promotion.getRedeemedCount() != null && promotion.getRedeemedCount() > 0) {
                        promotion.setRedeemedCount(promotion.getRedeemedCount() - 1);
                    }
                });
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking cancelled by customer: {}. {}", savedBooking.getBookingCode(), penaltyMsg);
        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CANCELLED,
                "Khách hàng hủy lịch hẹn",
                penaltyMsg));

        return mapToResponse(savedBooking);
    }

    private void validateBookingDate(LocalDate date, Customer customer) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Không thể chọn ngày trong quá khứ");
        }
        int windowDays = (customer != null && customer.getTier() != null && customer.getTier().getBookingWindowDays() != null)
                ? customer.getTier().getBookingWindowDays() : 7;
        LocalDate maxDate = LocalDate.now().plusDays(windowDays);
        if (date.isAfter(maxDate)) {
            throw new BadRequestException("Với hạng VIP hiện tại, bạn chỉ được phép đặt trước tối đa " + windowDays + " ngày (" + maxDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")!");
        }
    }

    private String generateUniqueBookingCode(LocalDate date) {
        String prefix = "NV-" + date.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            String code = prefix + String.format("%04d", random.nextInt(10000));
            if (!bookingRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        return prefix + System.currentTimeMillis() % 10000;
    }


    public BookingResponse mapToResponse(Booking entity) {
        List<BookingItemResponse> itemResponses = entity.getItems().stream()
                .map(item -> BookingItemResponse.builder()
                        .bookingItemId(item.getBookingItemId())
                        .serviceId(item.getServiceId())
                        .serviceCodeSnapshot(item.getServiceCodeSnapshot())
                        .serviceNameSnapshot(item.getServiceNameSnapshot())
                        .serviceTypeSnapshot(item.getServiceTypeSnapshot())
                        .priceSnapshot(item.getPriceSnapshot())
                        .build())
                .collect(Collectors.toList());

        String tierName = (entity.getCustomer() != null && entity.getCustomer().getTier() != null)
                ? entity.getCustomer().getTier().getTierName()
                : "Member";
        Integer points = (entity.getCustomer() != null && entity.getCustomer().getLoyaltyPoints() != null)
                ? entity.getCustomer().getLoyaltyPoints()
                : 0;

        return BookingResponse.builder()
                .bookingId(entity.getBookingId())
                .bookingCode(entity.getBookingCode())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                .customerName(entity.getCustomer() != null ? entity.getCustomer().getFullName() : "Khách hàng")
                .customerPhone(entity.getCustomer() != null ? entity.getCustomer().getPhoneNumber() : "")
                .customerTier(tierName)
                .customerPoints(points)
                .licensePlate(entity.getLicensePlate())
                .model(entity.getModel())
                .bookingDate(entity.getBookingDate())
                .timeSlotId(entity.getTimeSlot().getSlotId())
                .startTime(entity.getTimeSlot().getStartTime())
                .endTime(entity.getTimeSlot().getEndTime())
                .totalEstimatedAmount(entity.getTotalEstimatedAmount())
                .voucherCode(entity.getVoucherCode())
                .discountAmount(entity.getDiscountAmount())
                .finalAmount(entity.getFinalAmount() != null ? entity.getFinalAmount() : entity.getTotalEstimatedAmount())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .notes(entity.getNotes())
                .items(itemResponses)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> searchBookingsForAdmin(String query, LocalDate date) {
        if (query != null && !query.trim().isEmpty()) {
            return bookingRepository.searchBookings(query.trim())
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return bookingRepository.findAllByBookingDate(date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse checkinLate(String identifier) {
        Booking booking;
        try {
            Long bookingId = Long.parseLong(identifier);
            booking = bookingRepository.findById(bookingId)
                    .orElseGet(() -> bookingRepository.findByBookingCode(identifier)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch: " + identifier)));
        } catch (NumberFormatException e) {
            booking = bookingRepository.findByBookingCode(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch với mã: " + identifier));
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CANCELLED_NO_SHOW) {
            throw new BadRequestException("Đơn đặt lịch không ở trạng thái hợp lệ để cứu đơn (PENDING hoặc CANCELLED_NO_SHOW)");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Ràng buộc mốc thời gian cứu đơn linh hoạt trong ngày (Tối đa 180 phút)
        LocalTime maxLateTime = booking.getTimeSlot().getStartTime().plusMinutes(15);

        if (!booking.getBookingDate().isEqual(today)) {
            throw new BadRequestException("Không thể cứu đơn! Đơn đặt lịch thuộc ngày " + booking.getBookingDate() + ", không phải ngày hôm nay (" + today + ").");
        }

        if (now.isAfter(maxLateTime)) {
            throw new BadRequestException("Không thể khôi phục check-in! Đơn đặt lịch đã trễ quá 15 phút so với giờ bắt đầu slot (" + booking.getTimeSlot().getStartTime() + "). Quá hạn cứu đơn, bắt buộc phải đặt ca mới.");
        }

        // Kiểm tra công suất
        int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(booking.getBookingDate(), booking.getTimeSlot().getSlotId())
                .map(SlotLock::getLockCount)
                .orElse(0);

        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                booking.getBookingDate(), booking.getTimeSlot().getSlotId(), ACTIVE_CAPACITY_STATUSES);

        if (bookedCount + lockedCount >= booking.getTimeSlot().getMaxCapacity()) {
            throw new BadRequestException("Khung giờ này đã đầy công suất (Đã đặt: " + bookedCount + ", Đã khóa: " + lockedCount + "). Không thể check-in!");
        }

        booking.setStatus(BookingStatus.PENDING);
        Booking savedBooking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CHECKED_IN,
                "Cứu đơn check-in trễ thành công!",
                "Đơn " + booking.getBookingCode() + " đã khôi phục check-in trễ thành công tại quầy."));
        eventPublisher.publishEvent(new SlotCapacityChangeEvent(savedBooking.getBookingDate(), savedBooking.getTimeSlot().getSlotId()));

        return mapToResponse(savedBooking);
    }

    @Transactional
    public List<SlotOccupancyResponse> getOccupancyMonitor(LocalDate date) {
        List<TimeSlot> slots = timeSlotRepository.findAllByOrderByDisplayOrderAsc();
        List<SlotOccupancyResponse> responses = new ArrayList<>();

        for (TimeSlot slot : slots) {
            int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                    date, slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);

            int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slot.getSlotId())
                    .map(SlotLock::getLockCount)
                    .orElse(0);

            boolean isFullOrLocked = (bookedCount >= slot.getMaxCapacity()) || (lockedCount > 0) || !Boolean.TRUE.equals(slot.getIsActive());

            // Tự động ghi nhận lockCount trong DB nếu slot đã chạm hoặc vượt công suất tối đa
            if (bookedCount >= slot.getMaxCapacity() && lockedCount == 0) {
                SlotLock autoLock = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slot.getSlotId())
                        .orElseGet(() -> SlotLock.builder()
                                .lockDate(date)
                                .timeSlot(slot)
                                .lockCount(slot.getMaxCapacity())
                                .build());
                autoLock.setLockCount(slot.getMaxCapacity());
                slotLockRepository.save(autoLock);
            }

            responses.add(SlotOccupancyResponse.builder()
                    .slotId(slot.getSlotId())
                    .startTime(slot.getStartTime())
                    .maxCapacity(slot.getMaxCapacity())
                    .bookedCount(bookedCount)
                    .isActive(slot.getIsActive())
                    .isLocked(isFullOrLocked)
                    .build());
        }
        return responses;
    }

    @Transactional
    public SlotOccupancyResponse adjustLock(LocalDate date, Long slotId, boolean lock) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Không thể thiết lập khóa slot cho ngày trong quá khứ!");
        }

        // Ràng buộc nghiệp vụ: Không cho phép khóa slot nếu ngày đó đã đóng cửa toàn bộ trạm nghỉ lễ/bảo trì
        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(date);
        if (closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay())) {
            throw new BadRequestException("Ngày " + date + " đã đóng cửa toàn bộ trạm nghỉ lễ/bảo trì. Không cần khóa slot lẻ!");
        }

        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ với ID: " + slotId));

        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                date, slotId, ACTIVE_CAPACITY_STATUSES);

        SlotLock slotLock = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slotId)
                .orElseGet(() -> SlotLock.builder()
                        .lockDate(date)
                        .timeSlot(slot)
                        .lockCount(0)
                        .build());

        int newCount = lock ? Math.max(1, slot.getMaxCapacity() - bookedCount) : 0;
        if (lock && bookedCount >= slot.getMaxCapacity()) {
            newCount = slot.getMaxCapacity();
        }

        slotLock.setLockCount(newCount);
        slotLockRepository.save(slotLock);

        eventPublisher.publishEvent(new SlotCapacityChangeEvent(date, slotId));

        boolean isLocked = lock || (bookedCount >= slot.getMaxCapacity()) || (newCount > 0);

        return SlotOccupancyResponse.builder()
                .slotId(slot.getSlotId())
                .startTime(slot.getStartTime())
                .maxCapacity(slot.getMaxCapacity())
                .bookedCount(bookedCount)
                .isActive(slot.getIsActive())
                .isLocked(isLocked)
                .build();
    }

    @Transactional
    public BookingResponse completeCheckout(String identifier, String paymentMethod) {
        Booking booking;
        try {
            Long bookingId = Long.parseLong(identifier);
            booking = bookingRepository.findById(bookingId)
                    .orElseGet(() -> bookingRepository.findByBookingCode(identifier)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch: " + identifier)));
        } catch (NumberFormatException e) {
            booking = bookingRepository.findByBookingCode(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch với mã: " + identifier));
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Đơn đặt lịch này đã được thanh toán rồi!");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
//
//        if (booking.getBookingDate().isBefore(today)) {
//            throw new BadRequestException("Không thể thanh toán! Đơn đặt lịch thuộc ngày trong quá khứ (" + booking.getBookingDate() + ").");
//        }
//        if (booking.getBookingDate().isAfter(today)) {
//            throw new BadRequestException("Không thể thanh toán trước ngày hẹn! Đơn đặt lịch được hẹn cho ngày " + booking.getBookingDate() + ".");
//        }
//
//        LocalTime startTime = booking.getTimeSlot().getStartTime();
//        if (now.isBefore(startTime.minusMinutes(10))) {
//            throw new BadRequestException("Chưa đến giờ check-in (Chỉ cho phép thanh toán từ 10 phút trước giờ slot " + startTime + ").");
//        }
//        if (now.isAfter(startTime.plusMinutes(30)) && booking.getStatus() == BookingStatus.PENDING) {
//            throw new BadRequestException("Đã quá 5 phút so với giờ slot (" + startTime + "). Không thể thanh toán thường. Vui lòng sử dụng luồng Cứu Đơn.");
//        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking marked as COMPLETED and PAID via: {} for bookingId: {}", paymentMethod, booking.getBookingId());

        Customer customer = booking.getCustomer();
        BigDecimal amount = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalEstimatedAmount();
        
        com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig config = loyaltyConfigRepository.getGlobalConfig();
        BigDecimal baseSpend = config.getBasePointRate();
        BigDecimal basePoints = BigDecimal.valueOf(config.getBasePoints());
        BigDecimal multiplier = customer.getTier().getTierMultiplier() != null ? customer.getTier().getTierMultiplier() : BigDecimal.ONE;

        int pointsToAdd = amount.divide(baseSpend, 0, Boolean.TRUE.equals(config.getRoundDown()) ? java.math.RoundingMode.DOWN : java.math.RoundingMode.HALF_UP)
                .multiply(basePoints)
                .multiply(multiplier)
                .intValue();
        
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsToAdd);
        customer.setTotalSpending(customer.getTotalSpending().add(amount));
        customer.setTierSpending(customer.getTierSpending().add(amount));
        customer.setVisitCount(customer.getVisitCount() + 1);
        customer.setLastCompletedBookingAt(LocalDateTime.now());
        
        // Ghi nhận nhật ký tích điểm
        com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction pt = com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction.builder()
                .customer(customer)
                .points(pointsToAdd)
                .activityType(com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType.EARNED)
                .bookingCode(booking.getBookingCode())
                .build();
        pointTransactionRepository.save(pt);
        
        List<LoyaltyTier> allTiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
        BigDecimal tierSpend = customer.getTierSpending();

        for (int i = allTiers.size() - 1; i >= 0; i--) {
            LoyaltyTier tier = allTiers.get(i);
            if (tierSpend.compareTo(tier.getMinSpend()) >= 0) {
                if (customer.getTier() == null || customer.getTier().getMinSpend().compareTo(tier.getMinSpend()) < 0) {
                    customer.setTier(tier);
                    log.info("Customer {} upgraded to VIP tier: {} during checkout", customer.getCustomerId(), tier.getTierName());
                }
                break;
            }
        }
        
        customerRepository.save(customer);

        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.COMPLETED,
                "Giao dịch hoàn tất!",
                "Đơn hàng " + booking.getBookingCode() + " đã thanh toán thành công và hoàn thành dọn rửa. Bạn tích lũy được +" + pointsToAdd + " Pts."));

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForAdmin(LocalDate date, String status) {
        List<Booking> list;
        if (date != null) {
            list = bookingRepository.findAllByBookingDate(date);
        } else {
            list = bookingRepository.findAll();
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("All")) {
            String cleanStatus = status.trim().toLowerCase();
            list = list.stream().filter(b -> {
                String bStatus = b.getStatus().name().toLowerCase();
                if (cleanStatus.contains("cancel")) {
                    return bStatus.contains("cancel");
                }
                if (cleanStatus.contains("complete")) {
                    return bStatus.contains("complete");
                }
                if (cleanStatus.contains("pending") || cleanStatus.contains("confirm") || cleanStatus.contains("in_progress") || cleanStatus.contains("queue")) {
                    return bStatus.equals("pending") || bStatus.equals("confirmed") || bStatus.equals("in_progress");
                }
                return bStatus.equalsIgnoreCase(cleanStatus);
            }).collect(Collectors.toList());
        }

        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SlotLockResponse> getAllSlotLocks() {
        return slotLockRepository.findAll().stream()
                .filter(lock -> lock.getLockCount() > 0)
                .map(lock -> SlotLockResponse.builder()
                        .closureId(lock.getSlotLockId())
                        .closureDate(lock.getLockDate())
                        .reason("Khóa khung giờ " + lock.getTimeSlot().getStartTime())
                        .isFullDay(false)
                        .slotId(lock.getTimeSlot().getSlotId())
                        .startTime(lock.getTimeSlot().getStartTime().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * ?") // Chạy tự động vào lúc 00:00 nửa đêm hàng ngày
    @Transactional
    public void autoCleanPastClosuresAndLocks() {
        LocalDate today = LocalDate.now();
        try {
            int deletedClosures = garageClosureRepository.deleteByClosureDateBefore(today);
            int deletedLocks = slotLockRepository.deleteByLockDateBefore(today);
            if (deletedClosures > 0 || deletedLocks > 0) {
                log.info("Auto-cleanup completed: Deleted {} past closures and {} past slot locks.", deletedClosures, deletedLocks);
            }
        } catch (Exception e) {
            log.error("Failed to run scheduled auto-cleanup for past closures/locks", e);
        }
    }

    private int calculateBookingDurationMinutes(Booking booking) {
        if (booking.getItems() == null || booking.getItems().isEmpty()) {
            return 30;
        }
        int totalMinutes = 0;
        for (BookingItem item : booking.getItems()) {
            if (item.getServiceId() != null) {
                Optional<ServiceCatalog> scOpt = serviceCatalogRepository.findById(item.getServiceId());
                if (scOpt.isPresent() && scOpt.get().getDurationMinutes() != null) {
                    totalMinutes += scOpt.get().getDurationMinutes();
                }
            }
        }
        return totalMinutes > 0 ? totalMinutes : 30;
    }
}
