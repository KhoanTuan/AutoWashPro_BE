package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotAvailabilityResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import com.autowashpro.autowashpro_be.modules.booking.service.ServiceCatalogService;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerBookingController {

    private final BookingService bookingService;
    private final ServiceCatalogService serviceCatalogService;
    private final CustomerRepository customerRepository;

    @GetMapping("/services")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ServiceCatalogResponse>> getActiveServices() {
        return ResponseEntity.ok(serviceCatalogService.getAllServices(true));
    }

    @GetMapping("/slots")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<SlotAvailabilityResponse>> getAvailableSlots(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = principal != null ? customerRepository.findByEmail(principal.getUsername()).orElse(null) : null;
        return ResponseEntity.ok(bookingService.getAvailableSlots(date, customer));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = getAuthenticatedCustomer(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request, customer));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = getAuthenticatedCustomer(principal);
        return ResponseEntity.ok(bookingService.getCustomerBookings(customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = getAuthenticatedCustomer(principal);
        return ResponseEntity.ok(bookingService.getBookingById(id, customer));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Customer customer = getAuthenticatedCustomer(principal);
        return ResponseEntity.ok(bookingService.cancelBookingByCustomer(id, customer));
    }

    private Customer getAuthenticatedCustomer(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này!");
        }
        return customerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
    }
}
