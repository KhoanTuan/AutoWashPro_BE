package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity to track MoMo payment transactions for bookings.
 * Stores payment gateway responses and transaction details for audit and reconciliation.
 */
@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Payment gateway: "MOMO", "BANK_TRANSFER", "CASH", etc.
     */
    @Column(name = "payment_gateway", nullable = false, length = 50)
    private String paymentGateway;

    /**
     * Reference ID from MoMo (transId)
     */
    @Column(name = "momo_trans_id", length = 100)
    private String momoTransId;

    /**
     * Request ID sent to MoMo
     */
    @Column(name = "momo_request_id", length = 100)
    private String momoRequestId;

    /**
     * Order ID sent to MoMo
     */
    @Column(name = "momo_order_id", length = 100)
    private String momoOrderId;

    /**
     * Amount in VND
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Transaction status: "PENDING", "SUCCESS", "FAILED", "CANCELLED"
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /**
     * Result code from MoMo (0 = success, others indicate failure)
     */
    @Column(name = "result_code")
    private Integer resultCode;

    /**
     * Result message from MoMo API response
     */
    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    /**
     * Raw request payload sent to MoMo (for debugging and audit)
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * Raw response payload from MoMo (for debugging and audit)
     */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /**
     * Callback/IPN payload received from MoMo
     */
    @Column(name = "callback_payload", columnDefinition = "TEXT")
    private String callbackPayload;

    /**
     * Error details if payment processing failed
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
