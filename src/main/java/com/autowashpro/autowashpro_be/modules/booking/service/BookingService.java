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
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
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
            BookingStatus.IN_PROGRESS
    );

    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> getAvailableSlots(LocalDate date, Customer customer) {
        validateBookingDate(date, customer);

        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(date);
        boolean isClosedHoliday = closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay());
        String holidayReason = isClosedHoliday ? ("CLOSED_HOLIDAY: " + closureOpt.get().getReason()) : null;

        List<TimeSlot> slots = timeSlotRepository.findAllByOrderByDisplayOrderAsc();
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
            boolean isFull = availableCapacity <= 0;
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
                    .build());
        }

        return responses;
    }

    private boolean isSlotApplicableForDate(TimeSlot slot, LocalDate date) {
        String dowConfig = slot.getDayOfWeek();
        if (dowConfig == null || dowConfig.equalsIgnoreCase("ALL")) {
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
        String shortName = date.getDayOfWeek().name().substring(0, 3); // MON, TUE...
        return dowConfig.equalsIgnoreCase(shortName);
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Customer customer) {
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
            throw new BadRequestException("Tài khoản của bạn đã tự hủy " + canceledToday + " đơn đặt lịch trong ngày hôm nay. Để chống spam giữ chỗ, tài khoản bị tạm khóa tính năng đặt lịch cho đến ngày mai!");
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

        boolean isVehicleAlreadyBookedToday = bookingRepository.existsByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(
                request.getBookingDate(), vehicle.getLicensePlate(), ACTIVE_CAPACITY_STATUSES);
        if (isVehicleAlreadyBookedToday) {
            throw new BadRequestException("Xe mang biển số '" + vehicle.getLicensePlate() + "' hiện đang có 1 lịch hẹn giữ chỗ/chưa hoàn thành trong ngày " + 
                    request.getBookingDate() + ". Vui lòng hoàn thành dịch vụ và thanh toán xong cho lượt này (hoặc hủy lịch cũ) trước khi đặt lượt tiếp theo cho xe!");
        }


        ServiceCatalog packageService = serviceCatalogRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found with id: " + request.getPackageId()));
        if (!packageService.getIsActive() || packageService.getServiceType() != ServiceType.PACKAGE) {
            throw new BadRequestException("Gói rửa xe được chọn không hợp lệ hoặc đã ngừng kinh doanh");
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

        BigDecimal totalAmount = packageService.getPrice();
        booking.addItem(BookingItem.builder()
                .serviceId(packageService.getServiceId())
                .serviceCodeSnapshot(packageService.getServiceCode())
                .serviceNameSnapshot(packageService.getServiceName())
                .serviceTypeSnapshot(ServiceType.PACKAGE)
                .priceSnapshot(packageService.getPrice())
                .build());

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
                discountAmount = promotion.getValue();
                if (discountAmount.compareTo(totalAmount) > 0) {
                    discountAmount = totalAmount;
                }
            } else if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = totalAmount.multiply(promotion.getValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                // Áp dụng trần giảm tối đa (maxDiscountAmount) nếu có
                if (promotion.getMaxDiscountAmount() != null && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    if (discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                        discountAmount = promotion.getMaxDiscountAmount();
                    }
                }
            } else if (promotion.getDiscountType() == DiscountType.FREE_SERVICE) {
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

        // Chặn tự hủy sát giờ hẹn dưới 2 tiếng
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slotStartTime = LocalDateTime.of(booking.getBookingDate(), booking.getTimeSlot().getStartTime());
        if (now.plusHours(2).isAfter(slotStartTime)) {
            throw new BadRequestException("Không thể tự hủy lịch hẹn sát giờ phục vụ (dưới 2 tiếng). Vui lòng liên hệ Hotline xưởng để được hỗ trợ!");
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
        log.info("Booking cancelled by customer: {}", savedBooking.getBookingCode());
        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CANCELLED,
                "Khách hàng hủy lịch hẹn",
                "Khách hàng " + savedBooking.getCustomer().getFullName() + " đã hủy lịch hẹn " + savedBooking.getBookingCode() + " cho khung giờ ngày " + savedBooking.getBookingDate()));

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

        return BookingResponse.builder()
                .bookingId(entity.getBookingId())
                .bookingCode(entity.getBookingCode())
                .customerId(entity.getCustomer().getCustomerId())
                .customerName(entity.getCustomer().getFullName())
                .customerPhone(entity.getCustomer().getPhoneNumber())
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
    public BookingResponse checkinLate(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Đơn đặt lịch không ở trạng thái chờ phục vụ (PENDING)");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (booking.getBookingDate().isBefore(today) ||
                (booking.getBookingDate().isEqual(today) && now.isAfter(booking.getTimeSlot().getEndTime()))) {
            throw new BadRequestException("Không thể khôi phục check-in! Đơn đặt lịch đã quá giờ kết thúc slot (" + booking.getTimeSlot().getEndTime() + ").");
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

        booking.setStatus(BookingStatus.IN_PROGRESS);
        Booking savedBooking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CHECKED_IN,
                "Check-in trễ thành công!",
                "Đơn đặt lịch " + booking.getBookingCode() + " đã check-in trễ giờ thành công tại quầy và bắt đầu dọn rửa."));

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<SlotOccupancyResponse> getOccupancyMonitor(LocalDate date) {
        List<TimeSlot> slots = timeSlotRepository.findAllByOrderByDisplayOrderAsc();
        List<SlotOccupancyResponse> responses = new ArrayList<>();

        for (TimeSlot slot : slots) {
            int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                    date, slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);

            int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slot.getSlotId())
                    .map(SlotLock::getLockCount)
                    .orElse(0);

            boolean isLocked = (lockedCount > 0);

            responses.add(SlotOccupancyResponse.builder()
                    .slotId(slot.getSlotId())
                    .startTime(slot.getStartTime())
                    .maxCapacity(slot.getMaxCapacity())
                    .bookedCount(bookedCount)
                    .isActive(slot.getIsActive())
                    .isLocked(isLocked)
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

        if (lock && bookedCount >= slot.getMaxCapacity()) {
            throw new BadRequestException("Khung giờ này đã đầy công suất đặt lịch (" + bookedCount + "/" + slot.getMaxCapacity() + "). Không thể khóa thêm chỗ trống!");
        }

        SlotLock slotLock = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slotId)
                .orElseGet(() -> SlotLock.builder()
                        .lockDate(date)
                        .timeSlot(slot)
                        .lockCount(0)
                        .build());

        int newCount = lock ? Math.max(0, slot.getMaxCapacity() - bookedCount) : 0;

        slotLock.setLockCount(newCount);
        slotLockRepository.save(slotLock);

        eventPublisher.publishEvent(new SlotCapacityChangeEvent(date, slotId));

        boolean isLocked = (newCount > 0);

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
    public BookingResponse completeCheckout(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch: " + bookingId));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Đơn đặt lịch này đã được thanh toán rồi!");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking marked as COMPLETED and PAID via: {} for bookingId: {}", paymentMethod, bookingId);

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
}
