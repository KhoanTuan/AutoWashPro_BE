package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateAppointmentRequest;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "11 - Capacity & Scheduling", description = "Customer appointments and staff walk-in creation")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER') or hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(
            operationId = "11-02-create-booking",
            summary = "[CREATE] Initialize a new booking",
            description = """
                    Customers (`ROLE_CUSTOMER`) book with their authenticated profile and `vehicleId`.
                    Staff (`CREATE_WALK_IN_BOOKING`) may create walk-in or appointment bookings on behalf of customers.
                    Initial state: `BookingStatus.PENDING` + `PaymentStatus.UNPAID`.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Booking created",
            content = @Content(schema = @Schema(implementation = BookingResponse.class))
    )
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createAppointment(request));
    }
}
