package com.autowashpro.autowashpro_be.modules.financial.controller;

import com.autowashpro.autowashpro_be.modules.financial.dto.*;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.financial.service.AiAnalyticsService;
import com.autowashpro.autowashpro_be.modules.financial.service.FinancialLedgerService;
import com.autowashpro.autowashpro_be.modules.financial.service.InvoiceService;
import com.autowashpro.autowashpro_be.modules.financial.service.ShiftClosureService;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/financial/cashier")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "10 - Financial Cashier", description = "POS checkout, split-payment, and shift closeout")
public class CashierController {

    private static final String CASHIER_ACCESS = "hasAuthority('CASHIER_CHECKIN') or hasAuthority('ROLE_CASHIER')";

    private final InvoiceService invoiceService;
    private final ShiftClosureService shiftClosureService;
    private final AiAnalyticsService aiAnalyticsService;
    private final FinancialLedgerService financialLedgerService;
    private final BookingRepository bookingRepository;

    @PostMapping("/checkout")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(
            operationId = "10-02-checkout",
            summary = "[ACTION] Finalize checkout bill with optional split-payment",
            description = "Creates invoice, applies promotions, records cash portion, and initiates MoMo payment when requested."
    )
    @ApiResponse(responseCode = "201", description = "Invoice created",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class)))
    public ResponseEntity<InvoiceResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.checkout(request));
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-03-invoice-detail", summary = "[READ] Invoice detail")
    @ApiResponse(responseCode = "200", description = "Invoice retrieved",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class)))
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/cash")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-04-record-cash", summary = "[ACTION] Record cash payment on existing invoice")
    public ResponseEntity<InvoiceResponse> recordCash(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount
    ) {
        return ResponseEntity.ok(invoiceService.recordCashPayment(invoiceId, amount));
    }

    @PostMapping("/invoices/{invoiceId}/momo")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-05-initiate-momo", summary = "[ACTION] Initiate MoMo digital payment")
    public ResponseEntity<InvoiceResponse> initiateMomo(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount
    ) {
        return ResponseEntity.ok(invoiceService.initiateMomoForInvoice(invoiceId, amount));
    }

    /**
     * Counter checkout endpoint — manual cash/card registration for walk-in bookings.
     *
     * Secured via @PreAuthorize(CASHIER_ACCESS)
     *
     * Flow:
     * 1. Cashier scans or enters booking code
     * 2. System retrieves booking + calculates pricing
     * 3. Cashier records cash amount or initiates MoMo payment
     * 4. Invoice created, booking marked PAID
     * 5. Booking transitions to queue check-in
     */
    @PostMapping("/checkout/{bookingCode}")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(
            operationId = "10-11-counter-checkout-by-code",
            summary = "[ACTION] Counter checkout by booking code",
            description = """
                    Front desk manual checkout using booking code.
                    Supports split payment (cash + MoMo).
                    Booking status transitions from PENDING_PAYMENT to PAID upon successful payment.
                    """
    )
    @ApiResponse(responseCode = "201", description = "Invoice created and booking paid",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid booking code or payment amount")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    public ResponseEntity<InvoiceResponse> counterCheckoutByCode(
            @PathVariable String bookingCode,
            @Valid @RequestBody CheckoutRequest request
    ) {
        // Resolve booking by code instead of ID
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with code: " + bookingCode));

        // Set booking ID in request for standard checkout flow
        request.setBookingId(booking.getBookingId());

        // Delegate to existing checkout logic
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.checkout(request));
    }

    @PostMapping("/shift/open")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-06-open-shift", summary = "[ACTION] Open cashier shift")
    @ApiResponse(responseCode = "201", description = "Shift opened",
            content = @Content(schema = @Schema(implementation = ShiftClosureResponse.class)))
    public ResponseEntity<ShiftClosureResponse> openShift(@RequestBody(required = false) OpenShiftRequest request) {
        OpenShiftRequest body = request != null ? request : new OpenShiftRequest();
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftClosureService.openShift(body));
    }

    @GetMapping("/shift/current")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-07-current-shift", summary = "[READ] Current open shift")
    public ResponseEntity<ShiftClosureResponse> currentShift() {
        return ResponseEntity.ok(shiftClosureService.getCurrentShift());
    }

    @PostMapping("/shift/close")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(
            operationId = "10-08-close-shift",
            summary = "[ACTION] Close cashier shift",
            description = "Compares expected vs actual balance. Sets FLAGGED status when variance exceeds threshold."
    )
    public ResponseEntity<ShiftClosureResponse> closeShift(@Valid @RequestBody ShiftClosureRequest request) {
        return ResponseEntity.ok(shiftClosureService.closeShift(request));
    }

    @GetMapping("/analytics/recommendations")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(operationId = "10-09-ai-recommendations", summary = "[READ] AI management recommendations from ledger")
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved",
            content = @Content(schema = @Schema(implementation = AiRecommendationResponse.class)))
    public ResponseEntity<AiRecommendationResponse> getRecommendations() {
        return ResponseEntity.ok(aiAnalyticsService.getLatestRecommendations());
    }

    @PostMapping("/ledger/seal")
    @PreAuthorize(CASHIER_ACCESS)
    @Operation(
            operationId = "10-10-seal-ledger",
            summary = "[ACTION] Manually seal daily financial ledger",
            description = "Triggers the same sealing logic as the midnight cron job for the given business date."
    )
    public ResponseEntity<Map<String, Object>> sealLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate ledgerDate = date != null ? date : LocalDate.now();
        financialLedgerService.sealLedgerForDate(ledgerDate);
        return ResponseEntity.ok(Map.of(
                "ledgerDate", ledgerDate.toString(),
                "status", "SEALED"
        ));
    }
}
