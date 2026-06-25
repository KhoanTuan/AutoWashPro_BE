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
import com.autowashpro.autowashpro_be.modules.booking.state.BookingStateTransitionValidator;
import com.autowashpro.autowashpro_be.modules.capacity.service.SlotAvailabilityService;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAdminService;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import com.autowashpro.autowashpro_be.modules.operations.service.ChecklistService;
import com.autowashpro.autowashpro_be.modules.operations.service.QueueService;
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
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final SlotAvailabilityService slotAvailabilityService;
    private final BookingMapper mapper;
    private final QueueService queueService;
    private final ChecklistService checklistService;

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
                .pendingPayment(bookingRepository.search(BookingStatus.PENDING, today, null, PageRequest.of(0, 1)).getTotalElements())
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
                .bookingStatus(BookingStatus.PENDING)
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
    public BookingResponse createAppointment(CreateAppointmentRequest request) {
        UserPrincipal principal = requireAuthenticatedPrincipal();
        LocalDate bookingDate = LocalDate.parse(request.getBookingDate());
        slotAvailabilityService.ensureSlotAvailable(request.getSlotId(), bookingDate);

        if (principal.getUserType() == UserPrincipal.UserType.CUSTOMER) {
            return createAppointmentForCustomer(principal.getId(), request, bookingDate);
        }
        validateStaffWalkInRequest(request);
        return createBooking(toStaffWalkInRequest(request));
    }

    private void validateStaffWalkInRequest(CreateAppointmentRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new BadRequestException("customerName is required for staff walk-in booking");
        }
        if (request.getPlate() == null || request.getPlate().isBlank()) {
            throw new BadRequestException("plate is required for staff walk-in booking");
        }
    }

    private BookingResponse createAppointmentForCustomer(Long customerId, CreateAppointmentRequest request, LocalDate bookingDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Vehicle vehicle = resolveCustomerVehicle(customer, request);
        CarType carType = vehicle.getCarType();

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        WashService service = washServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        ServiceVariant variant = serviceVariantRepository.findByServiceServiceIdAndCarType(service.getServiceId(), carType)
                .orElseThrow(() -> new BadRequestException("No price variant for car type " + carType));

        BookingType bookingType = resolveBookingType(request.getBookingType(), BookingType.APP_BOOKING);

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .customer(customer)
                .vehicle(vehicle)
                .slot(slot)
                .bookingType(bookingType)
                .bookingDate(bookingDate)
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .notes(request.getNotes())
                .build();

        booking.getBookingItems().add(BookingItem.builder()
                .booking(booking)
                .variant(variant)
                .actualPrice(variant.getCalculatedPrice())
                .build());

        bookingRepository.save(booking);
        return mapper.toResponse(booking);
    }

    private Vehicle resolveCustomerVehicle(Customer customer, CreateAppointmentRequest request) {
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
            if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new BadRequestException("Vehicle does not belong to the authenticated customer");
            }
            return vehicle;
        }
        return vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(customer.getCustomerId())
                .orElseThrow(() -> new BadRequestException("vehicleId is required when no default vehicle exists"));
    }

    private CreateBookingRequest toStaffWalkInRequest(CreateAppointmentRequest request) {
        CreateBookingRequest staffRequest = new CreateBookingRequest();
        staffRequest.setBookingType(request.getBookingType());
        staffRequest.setCustomerId(request.getCustomerId());
        staffRequest.setCustomerName(request.getCustomerName());
        staffRequest.setPhone(request.getPhone());
        staffRequest.setEmail(request.getEmail());
        staffRequest.setPlate(request.getPlate());
        staffRequest.setVehicleType(request.getVehicleType());
        staffRequest.setServiceId(request.getServiceId());
        staffRequest.setSlotId(request.getSlotId());
        staffRequest.setBookingDate(request.getBookingDate());
        staffRequest.setNotes(request.getNotes());
        return staffRequest;
    }

    private BookingType resolveBookingType(String rawType, BookingType defaultType) {
        if (rawType == null || rawType.isBlank()) {
            return defaultType;
        }
        return "walk-in".equalsIgnoreCase(rawType) || "walkin".equalsIgnoreCase(rawType)
                ? BookingType.WALKIN
                : BookingType.APP_BOOKING;
    }

    private UserPrincipal requireAuthenticatedPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authentication required");
        }
        return principal;
    }

    @Transactional
    public BookingResponse markPaid(Long id) {
        Booking booking = findBooking(id);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.PAID);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.PAID);
        booking.setCashier(getCurrentStaff());
        bookingRepository.save(booking);
        queueService.checkIn(id);
        return hydrateAndMap(findBooking(id));
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
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.ASSIGNED);
        booking.setBookingStatus(BookingStatus.ASSIGNED);
        bookingRepository.save(booking);
        checklistService.initializeForBooking(booking, technician);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse accept(Long id) {
        Booking booking = findBooking(id);
        ensurePaid(booking);
        if (booking.getTaskChecklists().isEmpty()) {
            throw new BadRequestException("Assign technician before accepting");
        }
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.ACCEPTED);
        booking.setBookingStatus(BookingStatus.ACCEPTED);
        bookingRepository.save(booking);
        return hydrateAndMap(booking);
    }

    @Transactional
    public BookingResponse startProcessing(Long id) {
        Booking booking = findBooking(id);
        ensurePaid(booking);
        TaskChecklist task = booking.getTaskChecklists().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No technician assigned"));
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.PROCESSING);
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
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.COMPLETED);

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
        BigDecimal amount = booking.getCollectedRevenue();
        customer.setVisitCount(customer.getVisitCount() + 1);
        customer.setTotalSpending(customer.getTotalSpending().add(amount));
        customer.setLastCompletedBookingAt(LocalDateTime.now());

        queueService.markQueueCompleted(id);
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
        if ("PENDING".equals(normalized) || "PENDING_PAYMENT".equals(normalized)) {
            return BookingStatus.PENDING;
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
