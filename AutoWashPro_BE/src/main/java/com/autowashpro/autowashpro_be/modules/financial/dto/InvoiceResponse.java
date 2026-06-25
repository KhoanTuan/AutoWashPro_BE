package com.autowashpro.autowashpro_be.modules.financial.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long bookingId;
    private Long customerId;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal vehicleSurcharge;
    private BigDecimal promotionDiscount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaidCash;
    private BigDecimal amountPaidMomo;
    private BigDecimal remainingBalance;
    private String paymentStatus;
    private String splitPaymentStatus;
    private String invoiceStatus;
    private String momoPayUrl;
    private LocalDateTime createdAt;
}
