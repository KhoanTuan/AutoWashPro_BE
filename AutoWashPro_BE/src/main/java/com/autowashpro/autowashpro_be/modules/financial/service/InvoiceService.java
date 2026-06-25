package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.financial.dto.CheckoutRequest;
import com.autowashpro.autowashpro_be.modules.financial.dto.InvoiceResponse;
import com.autowashpro.autowashpro_be.modules.financial.dto.MomoPaymentResponse;
import com.autowashpro.autowashpro_be.modules.financial.dto.WebhookIpnPayload;
import com.autowashpro.autowashpro_be.modules.financial.entity.*;
import com.autowashpro.autowashpro_be.modules.financial.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.financial.repository.InvoiceRepository;
import com.autowashpro.autowashpro_be.modules.financial.repository.PaymentTransactionRepository;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.modules.operations.dto.DynamicPricingResultDto;
import com.autowashpro.autowashpro_be.modules.operations.service.DynamicPricingEngine;
import com.autowashpro.autowashpro_be.modules.operations.service.QueueService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final StaffRepository staffRepository;
    private final DynamicPricingEngine dynamicPricingEngine;
    private final MomoPaymentService momoPaymentService;
    private final QueueService queueService;
    private final ShiftClosureService shiftClosureService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {
        return toResponse(findInvoice(invoiceId), null);
    }

    @Transactional
    public InvoiceResponse checkout(CheckoutRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }

        invoiceRepository.findByBookingBookingId(booking.getBookingId()).ifPresent(existing -> {
            throw new BadRequestException("Invoice already exists for this booking");
        });

        PricingBreakdown pricing = calculatePricing(booking, request.getCustomerPromotionId());
        Staff cashier = getCurrentStaff();

        Invoice invoice = Invoice.builder()
                .invoiceCode(generateInvoiceCode())
                .booking(booking)
                .customer(booking.getCustomer())
                .cashier(cashier)
                .subtotal(pricing.subtotal())
                .vehicleSurcharge(pricing.vehicleSurcharge())
                .promotionDiscount(pricing.promotionDiscount())
                .totalAmount(pricing.totalAmount())
                .invoiceStatus(InvoiceStatus.FINALIZED)
                .paymentStatus(InvoicePaymentStatus.UNPAID)
                .splitPaymentStatus(SplitPaymentStatus.NONE)
                .notes(request.getNotes())
                .build();

        booking.finalizeRevenue(pricing.subtotal(), pricing.vehicleSurcharge(), pricing.totalAmount());
        invoiceRepository.save(invoice);

        String momoPayUrl = null;
        BigDecimal cashAmount = defaultZero(request.getCashAmount());
        BigDecimal momoAmount = defaultZero(request.getMomoAmount());

        if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
            recordCashPayment(invoice, cashAmount);
        }
        if (momoAmount.compareTo(BigDecimal.ZERO) > 0) {
            MomoPaymentResponse momoResponse = initiateMomoPayment(invoice, momoAmount);
            momoPayUrl = momoResponse.getPayUrl();
        }

        refreshSplitPaymentStatus(invoice);
        invoiceRepository.save(invoice);

        return toResponse(invoice, momoPayUrl);
    }

    @Transactional
    public InvoiceResponse recordCashPayment(Long invoiceId, BigDecimal amount) {
        Invoice invoice = findInvoice(invoiceId);
        ensureNotFullyPaid(invoice);
        recordCashPayment(invoice, amount);
        refreshSplitPaymentStatus(invoice);
        finalizeIfPaid(invoice);
        invoiceRepository.save(invoice);
        return toResponse(invoice, null);
    }

    @Transactional
    public InvoiceResponse initiateMomoForInvoice(Long invoiceId, BigDecimal amount) {
        Invoice invoice = findInvoice(invoiceId);
        ensureNotFullyPaid(invoice);
        MomoPaymentResponse response = initiateMomoPayment(invoice, amount);
        refreshSplitPaymentStatus(invoice);
        invoiceRepository.save(invoice);
        return toResponse(invoice, response.getPayUrl());
    }

    @Transactional
    public void handleMomoIpn(WebhookIpnPayload payload) {
        if (payload.getResultCode() == null) {
            throw new BadRequestException("Missing resultCode in MoMo IPN payload");
        }
        if (payload.getOrderId() == null || payload.getOrderId().isBlank()) {
            throw new BadRequestException("Missing orderId in MoMo IPN payload");
        }
        // Find the payment transaction by orderId (transaction_ref)
        PaymentTransaction tx = paymentTransactionRepository.findByTransactionRef(payload.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment transaction not found for orderId: " + payload.getOrderId()));
        // Update transaction metadata from MoMo response
        tx.setMomoResultCode(payload.getResultCode());
        tx.setMomoTransId(payload.getTransId() != null ? String.valueOf(payload.getTransId()) : null);
        tx.setRawResponse(toJson(payload));
        Invoice invoice = tx.getInvoice();
        Booking booking = invoice.getBooking();
        // ═══════════════════════════════════════════════════════════════════════════
        // CRITICAL: Booking Status Transition Logic (Phase 3 Requirement)
        // ═══════════════════════════════════════════════════════════════════════════
        if (payload.getResultCode() == 0) {
            // ─ Payment SUCCESS ─
            tx.setStatus(PaymentTxStatus.SUCCESS);

            // Update invoice amount paid via MoMo
            invoice.setAmountPaidMomo(invoice.getAmountPaidMomo().add(tx.getAmount()));

            // Recalculate split payment status
            refreshSplitPaymentStatus(invoice);

            // If invoice is now fully paid, finalize it
            finalizeIfPaid(invoice);

            // *** BOOKING STATUS TRANSITION ***
            // Transition: PENDING_PAYMENT → PAID (if booking is in PENDING_PAYMENT state)
            if (booking.getBookingStatus() == BookingStatus.PENDING) {
                booking.setBookingStatus(BookingStatus.PAID);
                booking.setPaymentStatus(PaymentStatus.PAID);
                log.info("Booking {} transitioned to PAID via MoMo IPN", booking.getBookingCode());
            }

            // Record MoMo collection in shift closure
            shiftClosureService.recordMomoCollection(tx.getAmount());

            log.info("MoMo payment SUCCESS: invoice={}, booking={}, amount={}",
                    invoice.getInvoiceCode(), booking.getBookingCode(), tx.getAmount());
        } else {
            // ─ Payment FAILED ─
            tx.setStatus(PaymentTxStatus.FAILED);
            log.warn("MoMo payment FAILED: orderId={}, resultCode={}, message={}",
                    payload.getOrderId(), payload.getResultCode(), payload.getMessage());
        }
        // Persist changes
        paymentTransactionRepository.save(tx);
        invoiceRepository.save(invoice);
        bookingRepository.save(booking);
    }

    private MomoPaymentResponse initiateMomoPayment(Invoice invoice, BigDecimal amount) {
        BigDecimal remaining = invoice.getRemainingBalance();
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException("MoMo amount exceeds remaining balance");
        }

        String orderId = "MOMO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String orderInfo = "AutoWashPro invoice " + invoice.getInvoiceCode();

        PaymentTransaction tx = PaymentTransaction.builder()
                .invoice(invoice)
                .transactionRef(orderId)
                .paymentMethod(PaymentMethod.MOMO)
                .amount(amount)
                .status(PaymentTxStatus.PENDING)
                .build();
        invoice.getPaymentTransactions().add(tx);
        paymentTransactionRepository.save(tx);

        return momoPaymentService.createPayment(orderId, amount, orderInfo);
    }

    private void recordCashPayment(Invoice invoice, BigDecimal amount) {
        BigDecimal remaining = invoice.getRemainingBalance();
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException("Cash amount exceeds remaining balance");
        }

        String ref = "CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PaymentTransaction tx = PaymentTransaction.builder()
                .invoice(invoice)
                .transactionRef(ref)
                .paymentMethod(PaymentMethod.CASH)
                .amount(amount)
                .status(PaymentTxStatus.SUCCESS)
                .build();
        invoice.getPaymentTransactions().add(tx);
        invoice.setAmountPaidCash(invoice.getAmountPaidCash().add(amount));
        paymentTransactionRepository.save(tx);
        shiftClosureService.recordCashCollection(amount);
    }

    private void finalizeIfPaid(Invoice invoice) {
        if (!invoice.isFullyPaid()) {
            invoice.setPaymentStatus(InvoicePaymentStatus.PARTIALLY_PAID);
            return;
        }

        invoice.setPaymentStatus(InvoicePaymentStatus.PAID);
        Booking booking = invoice.getBooking();
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setCashier(invoice.getCashier());
            bookingRepository.save(booking);
            queueService.checkIn(booking.getBookingId());
        }

        redeemPromotionIfApplied(invoice);
    }

    private void redeemPromotionIfApplied(Invoice invoice) {
        if (invoice.getPromotionDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        customerPromotionRepository.findTopByCustomerCustomerIdAndStatusOrderByCreatedAtDesc(
                invoice.getCustomer().getCustomerId(),
                CustomerPromotionStatus.AVAILABLE
        ).ifPresent(cp -> {
            cp.setStatus(CustomerPromotionStatus.REDEEMED);
            cp.setRedeemedAt(LocalDateTime.now());
            customerPromotionRepository.save(cp);
        });
    }

    private void refreshSplitPaymentStatus(Invoice invoice) {
        boolean hasCash = invoice.getAmountPaidCash().compareTo(BigDecimal.ZERO) > 0;
        boolean hasMomo = invoice.getAmountPaidMomo().compareTo(BigDecimal.ZERO) > 0;

        if (hasCash && hasMomo) {
            invoice.setSplitPaymentStatus(SplitPaymentStatus.SPLIT);
        } else if (hasCash) {
            invoice.setSplitPaymentStatus(SplitPaymentStatus.CASH_ONLY);
        } else if (hasMomo) {
            invoice.setSplitPaymentStatus(SplitPaymentStatus.MOMO_ONLY);
        } else {
            invoice.setSplitPaymentStatus(SplitPaymentStatus.NONE);
        }
    }

    private PricingBreakdown calculatePricing(Booking booking, Long customerPromotionId) {
        BigDecimal baseSubtotal = booking.getBookingItems().stream()
                .map(item -> item.getActualPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CarType carType = booking.getVehicle().getCarType();
        DynamicPricingResultDto dynamic = dynamicPricingEngine.calculateFromBasePrice(baseSubtotal, carType);

        BigDecimal promotionDiscount = BigDecimal.ZERO;
        if (customerPromotionId != null) {
            CustomerPromotion cp = customerPromotionRepository.findById(customerPromotionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer promotion not found"));
            if (cp.getStatus() != CustomerPromotionStatus.AVAILABLE) {
                throw new BadRequestException("Promotion is not available");
            }
            Promotion promotion = cp.getPromotion();
            if (promotion.getStatus() != PromotionStatus.ACTIVE) {
                throw new BadRequestException("Promotion is not active");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo())) {
                throw new BadRequestException("Promotion is outside valid date range");
            }
            promotionDiscount = calculateDiscount(promotion, dynamic.getFinalizedTotalPrice());
            cp.setAppliedAt(now);
            customerPromotionRepository.save(cp);
        }

        BigDecimal total = dynamic.getFinalizedTotalPrice().subtract(promotionDiscount).max(BigDecimal.ZERO);
        return new PricingBreakdown(
                dynamic.getBasePrice(),
                dynamic.getSurchargeAmount(),
                promotionDiscount,
                total
        );
    }

    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal baseAmount) {
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            return baseAmount.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        return promotion.getDiscountValue().min(baseAmount);
    }

    private Invoice findInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private void ensureNotFullyPaid(Invoice invoice) {
        if (invoice.getPaymentStatus() == InvoicePaymentStatus.PAID) {
            throw new BadRequestException("Invoice is already fully paid");
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

    private String generateInvoiceCode() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private InvoiceResponse toResponse(Invoice invoice, String momoPayUrl) {
        invoice.getCustomer().getFullName();
        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceCode(invoice.getInvoiceCode())
                .bookingId(invoice.getBooking().getBookingId())
                .customerId(invoice.getCustomer().getCustomerId())
                .customerName(invoice.getCustomer().getFullName())
                .subtotal(invoice.getSubtotal())
                .vehicleSurcharge(invoice.getVehicleSurcharge())
                .promotionDiscount(invoice.getPromotionDiscount())
                .totalAmount(invoice.getTotalAmount())
                .amountPaidCash(invoice.getAmountPaidCash())
                .amountPaidMomo(invoice.getAmountPaidMomo())
                .remainingBalance(invoice.getRemainingBalance())
                .paymentStatus(invoice.getPaymentStatus().name())
                .splitPaymentStatus(invoice.getSplitPaymentStatus().name())
                .invoiceStatus(invoice.getInvoiceStatus().name())
                .momoPayUrl(momoPayUrl)
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private record PricingBreakdown(
            BigDecimal subtotal,
            BigDecimal vehicleSurcharge,
            BigDecimal promotionDiscount,
            BigDecimal totalAmount
    ) {
    }
}
