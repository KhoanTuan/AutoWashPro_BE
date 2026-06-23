package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.*;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAdminService;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final WashServiceRepository washServiceRepository;
    private final ServiceVariantRepository serviceVariantRepository;
    private final StaffRepository staffRepository;
    private final CustomerAdminService customerAdminService;
    private final BookingMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listBookings(String status, String date, String keyword, int page, int size) {
        BookingStatus bookingStatus = parseBookingStatus(status);
        LocalDate bookingDate = date != null && !date.isBlank() ? LocalDate.parse(date) : null;

        Page<Booking> result = bookingRepository.search(
                bookingStatus,
                bookingDate,
                keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<BookingResponse> content = result.getContent().stream()
                .map(this::hydrateAndMap)
                .toList();

        return PageResponse.<BookingResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingSummaryStatsResponse getSummaryStats() {
        LocalDate today = LocalDate.now();
        return BookingSummaryStatsResponse.builder()
                .todayTotal(bookingRepository.countByBookingDate(today))
                .todayWalkIns(bookingRepository.countByBookingDateAndBookingType(today, BookingType.WALKIN))
                .pendingPayment(bookingRepository.search(BookingStatus.PENDING_PAYMENT, today, null, PageRequest.of(0, 1)).getTotalElements())
                .inProgress(bookingRepository.search(BookingStatus.PROCESSING, today, null, PageRequest.of(0, 1)).getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> listServices() {
        return washServiceRepository.findAll(Sort.by("serviceId").ascending()).stream()
                .map(mapper::toCatalog)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotOptionResponse> listSlots() {
        return slotRepository.findAll(Sort.by("startTime").ascending()).stream()
                .map(mapper::toSlotOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long id) {
        return hydrateAndMap(findBooking(id));
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        CarType carType = parseCarType(request.getVehicleType());
        Customer customer = customerAdminService.resolveOrCreateForBooking(
                request.getCustomerId(),
                request.getCustomerName(),
                request.getPhone(),
                request.getEmail(),
                request.getPlate(),
                carType
        );
        Vehicle vehicle = customerAdminService.resolveVehicle(customer, request.getPlate(), carType);

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        WashService service = washServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        ServiceVariant variant = serviceVariantRepository.findByServiceServiceIdAndCarType(service.getServiceId(), carType)
                .orElseThrow(() -> new BadRequestException("No price variant for car type " + carType));

        BookingType bookingType = "appt".equalsIgnoreCase(request.getBookingType())
                ? BookingType.APP_BOOKING
                : BookingType.WALKIN;

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .customer(customer)
                .vehicle(vehicle)
                .slot(slot)
                .bookingType(bookingType)
                .bookingDate(LocalDate.parse(request.getBookingDate()))
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .notes(request.getNotes())
                .build();

        BookingItem item = BookingItem.builder()
                .booking(booking)
                .variant(variant)
                .actualPrice(variant.getCalculatedPrice())
                .build();
        booking.getBookingItems().add(item);

        bookingRepository.save(booking);
        return mapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse markPaid(Long id) {
        Booking booking = findBooking(id);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setCashier(getCurrentStaff());
        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse assignStaff(Long id, Long technicianId) {
        Booking booking = findBooking(id);
        ensurePaid(booking);

        if (!booking.getTaskChecklists().isEmpty()) {
            throw new BadRequestException("Technician already assigned");
        }

        Staff technician = staffRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        if (technician.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Technician is not active");
        }

        TaskChecklist task = TaskChecklist.builder()
                .booking(booking)
                .technician(technician)
                .status(TaskStatus.NOT_STARTED)
                .build();
        booking.getTaskChecklists().add(task);
        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse accept(Long id) {
        Booking booking = findBooking(id);
        ensurePaid(booking);
        if (booking.getTaskChecklists().isEmpty()) {
            throw new BadRequestException("Assign technician before accepting");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Booking cannot be accepted in current status");
        }
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse startProcessing(Long id) {
        Booking booking = findBooking(id);
        ensurePaid(booking);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking must be confirmed before processing");
        }

        TaskChecklist task = booking.getTaskChecklists().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No technician assigned"));
        task.setStatus(TaskStatus.PROCESSING);
        task.setStartTime(LocalDateTime.now());

        Staff technician = task.getTechnician();
        technician.setWorkStatus(StaffWorkStatus.BUSY);
        staffRepository.save(technician);

        booking.setBookingStatus(BookingStatus.PROCESSING);
        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse complete(Long id) {
        Booking booking = findBooking(id);
        ensurePaid(booking);
        if (booking.getBookingStatus() != BookingStatus.PROCESSING) {
            throw new BadRequestException("Booking must be processing before complete");
        }

        TaskChecklist task = booking.getTaskChecklists().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No technician assigned"));
        task.setStatus(TaskStatus.DONE);
        task.setEndTime(LocalDateTime.now());

        Staff technician = task.getTechnician();
        technician.setWorkStatus(StaffWorkStatus.IDLE);
        technician.setTotalJobsCompleted(
                (technician.getTotalJobsCompleted() != null ? technician.getTotalJobsCompleted() : 0) + 1
        );
        staffRepository.save(technician);

        booking.setBookingStatus(BookingStatus.COMPLETED);

        Customer customer = booking.getCustomer();
        BigDecimal amount = booking.getBookingItems().stream()
                .map(BookingItem::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        customer.setVisitCount(customer.getVisitCount() + 1);
        customer.setTotalSpending(customer.getTotalSpending().add(amount));
        customer.setLastCompletedBookingAt(LocalDateTime.now());

        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    private BookingResponse hydrateAndMap(Booking booking) {
        booking.getCustomer().getFullName();
        booking.getVehicle().getLicensePlate();
        booking.getSlot().getStartTime();
        booking.getBookingItems().forEach(i -> i.getVariant().getService().getServiceName());
        booking.getTaskChecklists().forEach(t -> t.getTechnician().getFullName());
        return mapper.toResponse(booking);
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void ensurePaid(Booking booking) {
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Payment required before this action");
        }
    }

    private Staff getCurrentStaff() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        if (principal.getUserType() != UserPrincipal.UserType.STAFF) {
            return null;
        }
        return staffRepository.findById(principal.getId()).orElse(null);
    }

    private BookingStatus parseBookingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase().replace(' ', '_');
        if ("PENDING".equals(normalized)) {
            return BookingStatus.PENDING_PAYMENT;
        }
        return BookingStatus.valueOf(normalized);
    }

    private CarType parseCarType(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            return CarType.SEDAN;
        }
        String v = vehicleType.toUpperCase();
        if ("VAN".equals(v)) {
            return CarType.SUV;
        }
        return CarType.valueOf(v);
    }

    private String generateBookingCode() {
        return "AWB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
