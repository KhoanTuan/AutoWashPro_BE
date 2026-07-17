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
import com.autowashpro.autowashpro_be.security.UserPrincipal;
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

        // Kiá»ƒm tra xem khÃ¡ch hÃ ng cÃ³ Ä‘ang bá»‹ pháº¡t khÃ³a Ä‘áº·t lá»‹ch 7 ngÃ y hay khÃ´ng (do trá»… háº¹n No-Show >= 3 láº§n trong vÃ²ng 30 ngÃ y)
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
                    throw new BadRequestException("TÃ i khoáº£n cá»§a báº¡n Ä‘Ã£ bá»‹ táº¡m khÃ³a tÃ­nh nÄƒng Ä‘áº·t lá»‹ch online Ä‘áº¿n " + banUntil.format(formatter) + " do vi pháº¡m trá»… háº¹n (No-Show) " + noShowCount + " láº§n trong vÃ²ng 30 ngÃ y qua!");
                }
            }
        }

        // Cháº·n spam Ä‘áº·t/há»§y liÃªn tá»¥c trong ngÃ y (tá»‘i Ä‘a 3 láº§n/ngÃ y)
        java.time.LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long canceledToday = bookingRepository.countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(
                customer.getCustomerId(), 
                Arrays.asList(BookingStatus.CANCELLED_BY_CUSTOMER, BookingStatus.CANCELLED_NO_SHOW), 
                startOfToday
        );
        if (canceledToday >= 3) {
            throw new BadRequestException("TÃ i khoáº£n cá»§a báº¡n Ä‘Ã£ tá»± há»§y " + canceledToday + " Ä‘Æ¡n Ä‘áº·t lá»‹ch trong ngÃ y hÃ´m nay. Äá»ƒ chá»‘ng spam giá»¯ chá»—, tÃ i khoáº£n bá»‹ táº¡m khÃ³a tÃ­nh nÄƒng Ä‘áº·t lá»‹ch cho Ä‘áº¿n ngÃ y mai!");
        }

        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(request.getBookingDate());
        if (closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay())) {
            throw new BadRequestException("XÆ°á»Ÿng Ä‘Ã³ng cá»­a nghá»‰ lá»… trong ngÃ y " + request.getBookingDate() + ". LÃ½ do: " + closureOpt.get().getReason());
        }

        TimeSlot slot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + request.getTimeSlotId()));

        if (!slot.getIsActive()) {
            throw new BadRequestException("Khung giá» nÃ y Ä‘ang táº¡m ngÆ°ng phá»¥c vá»¥ hoáº·c báº£o trÃ¬");
        }

        if (!isSlotApplicableForDate(slot, request.getBookingDate())) {
            throw new BadRequestException("Khung giá» nÃ y khÃ´ng Ã¡p dá»¥ng cho thá»©/ngÃ y Ä‘Æ°á»£c chá»n (" + request.getBookingDate().getDayOfWeek() + ")");
        }

        if (request.getBookingDate().isEqual(LocalDate.now()) && slot.getStartTime().isBefore(LocalTime.now())) {
            throw new BadRequestException("Khung giá» nÃ y Ä‘Ã£ qua trong ngÃ y hÃ´m nay");
        }

        int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(request.getBookingDate(), slot.getSlotId())
                .map(SlotLock::getLockCount)
                .orElse(0);
        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                request.getBookingDate(), slot.getSlotId(), ACTIVE_CAPACITY_STATUSES);
        if (bookedCount + lockedCount >= slot.getMaxCapacity()) {
            throw new BadRequestException("Khung giá» nÃ y Ä‘Ã£ Ä‘áº§y xe (ÄÃ£ Ä‘áº·t: " + bookedCount + ", ÄÃ£ khÃ³a: " + lockedCount + "/" + slot.getMaxCapacity() + "), vui lÃ²ng chá»n khung giá» khÃ¡c!");
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
            throw new BadRequestException("TÃ i khoáº£n cá»§a báº¡n Ä‘ang cÃ³ " + customerTodayBookings + "/" + maxDailyBookings + " Ä‘Æ¡n Ä‘áº·t lá»‹ch Ä‘ang giá»¯ chá»—/chÆ°a hoÃ n thÃ nh trong ngÃ y " + 
                    request.getBookingDate() + " (giá»›i háº¡n theo háº¡ng " + tierName + "). Vui lÃ²ng hoÃ n thÃ nh dá»‹ch vá»¥ vÃ  thanh toÃ¡n (hoáº·c há»§y lá»‹ch cÅ©) trÆ°á»›c khi Ä‘áº·t thÃªm!");
        }


        String cleanPlate = request.getLicensePlate() != null ? request.getLicensePlate().trim().toUpperCase() : "";
        if (cleanPlate.length() < 5 || cleanPlate.length() > 20) {
            throw new BadRequestException("Biá»ƒn sá»‘ xe pháº£i tá»« 5 Ä‘áº¿n 20 kÃ½ tá»± (vÃ­ dá»¥: 29-H1 555.55)");
        }
        Optional<Vehicle> existingVehOpt = vehicleRepository.findByLicensePlateIgnoreCase(cleanPlate);
        if (existingVehOpt.isEmpty() || !existingVehOpt.get().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BadRequestException("Biá»ƒn sá»‘ xe '" + cleanPlate + "' chÆ°a cÃ³ trong danh sÃ¡ch xe (Garage) cá»§a báº¡n! Vui lÃ²ng thÃªm xe vÃ o Garage trÆ°á»›c khi Ä‘áº·t lá»‹ch rá»­a.");
        }
        Vehicle vehicle = existingVehOpt.get();

        boolean isVehicleAlreadyBookedToday = bookingRepository.existsByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(
                request.getBookingDate(), vehicle.getLicensePlate(), ACTIVE_CAPACITY_STATUSES);
        if (isVehicleAlreadyBookedToday) {
            throw new BadRequestException("Xe mang biá»ƒn sá»‘ '" + vehicle.getLicensePlate() + "' hiá»‡n Ä‘ang cÃ³ 1 lá»‹ch háº¹n giá»¯ chá»—/chÆ°a hoÃ n thÃ nh trong ngÃ y " + 
                    request.getBookingDate() + ". Vui lÃ²ng hoÃ n thÃ nh dá»‹ch vá»¥ vÃ  thanh toÃ¡n xong cho lÆ°á»£t nÃ y (hoáº·c há»§y lá»‹ch cÅ©) trÆ°á»›c khi Ä‘áº·t lÆ°á»£t tiáº¿p theo cho xe!");
        }


        ServiceCatalog packageService = serviceCatalogRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found with id: " + request.getPackageId()));
        if (!packageService.getIsActive() || packageService.getServiceType() != ServiceType.PACKAGE) {
            throw new BadRequestException("GÃ³i rá»­a xe Ä‘Æ°á»£c chá»n khÃ´ng há»£p lá»‡ hoáº·c Ä‘Ã£ ngá»«ng kinh doanh");
        }

        Booking booking = Booking.builder()
                .bookingCode(generateUniqueBookingCode(request.getBookingDate()))
                .customer(customer)
                .licensePlate(vehicle.getLicensePlate())
                .model(vehicle.getModel() != null ? vehicle.getModel() : (request.getModel() != null ? request.getModel().trim() : "Xe mÃ¡y"))

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
                    throw new BadRequestException("Dá»‹ch vá»¥ thÃªm khÃ´ng há»£p lá»‡ hoáº·c Ä‘Ã£ ngá»«ng kinh doanh: " + addonId);
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
                    .orElseThrow(() -> new BadRequestException("MÃ£ giáº£m giÃ¡ '" + code + "' khÃ´ng tá»“n táº¡i trong vÃ­ cá»§a báº¡n!"));

            if (customerPromotion.getStatus() != CustomerPromotionStatus.ISSUED) {
                throw new BadRequestException("MÃ£ giáº£m giÃ¡ '" + code + "' Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng hoáº·c Ä‘Ã£ háº¿t háº¡n!");
            }

            Promotion promotion = customerPromotion.getPromotion();
            if (promotion.getStatus() != PromotionStatus.ACTIVE) {
                throw new BadRequestException("Chiáº¿n dá»‹ch khuyáº¿n mÃ£i cho mÃ£ nÃ y Ä‘Ã£ káº¿t thÃºc hoáº·c táº¡m ngÆ°ng!");
            }

            if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("MÃ£ giáº£m giÃ¡ '" + code + "' Ä‘Ã£ quÃ¡ háº¡n sá»­ dá»¥ng!");
            }

            // 1. RÃ ng buá»™c gÃ³i dá»‹ch vá»¥ (applicableServiceCode)
            if (promotion.getApplicableServiceCode() != null && !promotion.getApplicableServiceCode().trim().isEmpty()) {
                String reqServiceCode = packageService.getServiceCode();
                if (!promotion.getApplicableServiceCode().trim().equalsIgnoreCase(reqServiceCode)) {
                    throw new BadRequestException("MÃ£ Æ°u Ä‘Ã£i nÃ y chá»‰ Ã¡p dá»¥ng cho gÃ³i dá»‹ch vá»¥: " + promotion.getApplicableServiceCode());
                }
            }

            // 2. RÃ ng buá»™c ngÃ y trong tuáº§n (applicableDays)
            if (promotion.getApplicableDays() != null && !promotion.getApplicableDays().trim().isEmpty()) {
                String bookingDayOfWeek = request.getBookingDate().getDayOfWeek().name().substring(0, 3).toUpperCase();
                String appDays = promotion.getApplicableDays().toUpperCase();
                if (!appDays.contains(bookingDayOfWeek)) {
                    throw new BadRequestException("MÃ£ Æ°u Ä‘Ã£i nÃ y chá»‰ Ã¡p dá»¥ng cho cÃ¡c ngÃ y: " + promotion.getApplicableDays());
                }
            }

            // 3. RÃ ng buá»™c giÃ¡ trá»‹ Ä‘Æ¡n hÃ ng tá»‘i thiá»ƒu (minOrderValue)
            // Chá»‰ Ã¡p dá»¥ng cho cÃ¡c voucher tiáº¿p thá»‹/phÃ¡t táº·ng miá»…n phÃ­ (costPoints == 0),
            // bá» qua cho cÃ¡c voucher Ä‘á»•i báº±ng Ä‘iá»ƒm Loyalty (Ä‘Ã£ tá»± tráº£ giÃ¡ báº±ng Ä‘iá»ƒm).
            if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                boolean isPointsExchange = (promotion.getCostPoints() != null && promotion.getCostPoints() > 0)
                        || (customerPromotion.getSource() == CustomerPromotionSource.EXCHANGE);
                if (!isPointsExchange && totalAmount.compareTo(promotion.getMinOrderValue()) < 0) {
                    throw new BadRequestException("MÃ£ Æ°u Ä‘Ã£i nÃ y chá»‰ Ã¡p dá»¥ng cho Ä‘Æ¡n hÃ ng tá»« " + 
                            promotion.getMinOrderValue().setScale(0, java.math.RoundingMode.HALF_UP).toString() + " Ä‘ trá»Ÿ lÃªn!");
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
                // Ãp dá»¥ng tráº§n giáº£m tá»‘i Ä‘a (maxDiscountAmount) náº¿u cÃ³
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

            // Táº¡m thá»i Ä‘Ã¡nh dáº¥u Ä‘Ã£ dÃ¹ng Ä‘á»ƒ khÃ³a voucher trÃ¡nh double claim
            customerPromotion.setStatus(CustomerPromotionStatus.USED);
            customerPromotionRepository.save(customerPromotion);

            // TÄƒng sá»‘ lÆ°á»£ng Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng thá»±c táº¿ cá»§a chiáº¿n dá»‹ch
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
    public BookingResponse getBookingById(Long id, UserPrincipal principal) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isStaff) {
            if (!booking.getCustomer().getCustomerId().equals(principal.getId())) {
                throw new BadRequestException("Bu00e1n khu00f4ng cu00f3 quyu00e1n xem u0111u01a1n u0111u00e1t lu00edch nu00e0y!");
            }
        }
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBookingByCustomer(Long id, Customer customer) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if (!booking.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BadRequestException("Báº¡n khÃ´ng cÃ³ quyá»n thao tÃ¡c trÃªn Ä‘Æ¡n Ä‘áº·t lá»‹ch nÃ y!");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("ÄÆ¡n Ä‘áº·t lá»‹ch Ä‘ang á»Ÿ tráº¡ng thÃ¡i '" + booking.getStatus() + "', khÃ´ng thá»ƒ há»§y!");
        }

        // Cháº·n tá»± há»§y sÃ¡t giá» háº¹n dÆ°á»›i 2 tiáº¿ng
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slotStartTime = LocalDateTime.of(booking.getBookingDate(), booking.getTimeSlot().getStartTime());
        if (now.plusHours(2).isAfter(slotStartTime)) {
            throw new BadRequestException("KhÃ´ng thá»ƒ tá»± há»§y lá»‹ch háº¹n sÃ¡t giá» phá»¥c vá»¥ (dÆ°á»›i 2 tiáº¿ng). Vui lÃ²ng liÃªn há»‡ Hotline xÆ°á»Ÿng Ä‘á»ƒ Ä‘Æ°á»£c há»— trá»£!");
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
                "KhÃ¡ch hÃ ng há»§y lá»‹ch háº¹n",
                "KhÃ¡ch hÃ ng " + savedBooking.getCustomer().getFullName() + " Ä‘Ã£ há»§y lá»‹ch háº¹n " + savedBooking.getBookingCode() + " cho khung giá» ngÃ y " + savedBooking.getBookingDate()));

        return mapToResponse(savedBooking);
    }

    private void validateBookingDate(LocalDate date, Customer customer) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("KhÃ´ng thá»ƒ chá»n ngÃ y trong quÃ¡ khá»©");
        }
        int windowDays = (customer != null && customer.getTier() != null && customer.getTier().getBookingWindowDays() != null)
                ? customer.getTier().getBookingWindowDays() : 7;
        LocalDate maxDate = LocalDate.now().plusDays(windowDays);
        if (date.isAfter(maxDate)) {
            throw new BadRequestException("Vá»›i háº¡ng VIP hiá»‡n táº¡i, báº¡n chá»‰ Ä‘Æ°á»£c phÃ©p Ä‘áº·t trÆ°á»›c tá»‘i Ä‘a " + windowDays + " ngÃ y (" + maxDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")!");
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
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n Ä‘áº·t lá»‹ch: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("ÄÆ¡n Ä‘áº·t lá»‹ch khÃ´ng á»Ÿ tráº¡ng thÃ¡i chá» phá»¥c vá»¥ (PENDING)");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (booking.getBookingDate().isBefore(today) ||
                (booking.getBookingDate().isEqual(today) && now.isAfter(booking.getTimeSlot().getEndTime()))) {
            throw new BadRequestException("KhÃ´ng thá»ƒ khÃ´i phá»¥c check-in! ÄÆ¡n Ä‘áº·t lá»‹ch Ä‘Ã£ quÃ¡ giá» káº¿t thÃºc slot (" + booking.getTimeSlot().getEndTime() + ").");
        }

        // Kiá»ƒm tra cÃ´ng suáº¥t
        int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(booking.getBookingDate(), booking.getTimeSlot().getSlotId())
                .map(SlotLock::getLockCount)
                .orElse(0);

        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                booking.getBookingDate(), booking.getTimeSlot().getSlotId(), ACTIVE_CAPACITY_STATUSES);

        if (bookedCount + lockedCount >= booking.getTimeSlot().getMaxCapacity()) {
            throw new BadRequestException("Khung giá» nÃ y Ä‘Ã£ Ä‘áº§y cÃ´ng suáº¥t (ÄÃ£ Ä‘áº·t: " + bookedCount + ", ÄÃ£ khÃ³a: " + lockedCount + "). KhÃ´ng thá»ƒ check-in!");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        Booking savedBooking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvent(this, savedBooking, BookingEventAction.CHECKED_IN,
                "Check-in trá»… thÃ nh cÃ´ng!",
                "ÄÆ¡n Ä‘áº·t lá»‹ch " + booking.getBookingCode() + " Ä‘Ã£ check-in trá»… giá» thÃ nh cÃ´ng táº¡i quáº§y vÃ  báº¯t Ä‘áº§u dá»n rá»­a."));

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
            throw new BadRequestException("KhÃ´ng thá»ƒ thiáº¿t láº­p khÃ³a slot cho ngÃ y trong quÃ¡ khá»©!");
        }

        // RÃ ng buá»™c nghiá»‡p vá»¥: KhÃ´ng cho phÃ©p khÃ³a slot náº¿u ngÃ y Ä‘Ã³ Ä‘Ã£ Ä‘Ã³ng cá»­a toÃ n bá»™ tráº¡m nghá»‰ lá»…/báº£o trÃ¬
        Optional<GarageClosure> closureOpt = garageClosureRepository.findByClosureDate(date);
        if (closureOpt.isPresent() && Boolean.TRUE.equals(closureOpt.get().getIsFullDay())) {
            throw new BadRequestException("NgÃ y " + date + " Ä‘Ã£ Ä‘Ã³ng cá»­a toÃ n bá»™ tráº¡m nghá»‰ lá»…/báº£o trÃ¬. KhÃ´ng cáº§n khÃ³a slot láº»!");
        }

        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y khung giá» vá»›i ID: " + slotId));

        int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                date, slotId, ACTIVE_CAPACITY_STATUSES);

        if (lock && bookedCount >= slot.getMaxCapacity()) {
            throw new BadRequestException("Khung giá» nÃ y Ä‘Ã£ Ä‘áº§y cÃ´ng suáº¥t Ä‘áº·t lá»‹ch (" + bookedCount + "/" + slot.getMaxCapacity() + "). KhÃ´ng thá»ƒ khÃ³a thÃªm chá»— trá»‘ng!");
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
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n Ä‘áº·t lá»‹ch: " + bookingId));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("ÄÆ¡n Ä‘áº·t lá»‹ch nÃ y Ä‘Ã£ Ä‘Æ°á»£c thanh toÃ¡n rá»“i!");
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
        
        // Ghi nháº­n nháº­t kÃ½ tÃ­ch Ä‘iá»ƒm
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
                "Giao dá»‹ch hoÃ n táº¥t!",
                "ÄÆ¡n hÃ ng " + booking.getBookingCode() + " Ä‘Ã£ thanh toÃ¡n thÃ nh cÃ´ng vÃ  hoÃ n thÃ nh dá»n rá»­a. Báº¡n tÃ­ch lÅ©y Ä‘Æ°á»£c +" + pointsToAdd + " Pts."));

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
                        .reason("KhÃ³a khung giá» " + lock.getTimeSlot().getStartTime())
                        .isFullDay(false)
                        .slotId(lock.getTimeSlot().getSlotId())
                        .startTime(lock.getTimeSlot().getStartTime().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * ?") // Cháº¡y tá»± Ä‘á»™ng vÃ o lÃºc 00:00 ná»­a Ä‘Ãªm hÃ ng ngÃ y
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




