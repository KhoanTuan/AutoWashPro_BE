package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingItemResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotAvailabilityResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.GarageClosureRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceCatalogRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import org.springframework.stereotype.Service;
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
    private final RealtimeNotificationService notificationService;
    private final CustomerPromotionRepository customerPromotionRepository;

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

            int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                    date, slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);
            int availableCapacity = Math.max(0, slot.getMaxCapacity() - bookedCount);

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

        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                request.getBookingDate(), slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);
        if (bookedCount >= slot.getMaxCapacity()) {
            throw new BadRequestException("Khung giờ này đã đầy xe (" + bookedCount + "/" + slot.getMaxCapacity() + "), vui lòng chọn khung giờ khác!");
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

            // Calculate discount
            if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                discountAmount = promotion.getValue();
                if (discountAmount.compareTo(totalAmount) > 0) {
                    discountAmount = totalAmount;
                }
            } else if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = totalAmount.multiply(promotion.getValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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
        notificationService.notifyNewBooking(savedBooking);

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
        notificationService.notifyBookingStatusChanged(savedBooking, NotificationType.BOOKING_CANCELLED, "Khách hàng hủy lịch hẹn", "Khách hàng " + savedBooking.getCustomer().getFullName() + " đã hủy lịch hẹn " + savedBooking.getBookingCode() + " cho khung giờ ngày " + savedBooking.getBookingDate());

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
}
