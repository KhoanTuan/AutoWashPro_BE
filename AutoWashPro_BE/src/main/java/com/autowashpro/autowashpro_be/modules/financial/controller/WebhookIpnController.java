package com.autowashpro.autowashpro_be.modules.financial.controller;
import com.autowashpro.autowashpro_be.modules.financial.dto.WebhookIpnPayload;
import com.autowashpro.autowashpro_be.modules.financial.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/v1/financial/webhook")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "10 - Financial Webhook", description = "Public MoMo IPN callback — no JWT required")
public class WebhookIpnController {
    private final InvoiceService invoiceService;
    /**
     * MoMo async payment notification (IPN) handler.
     *
     * When resultCode == 0:
     * - Payment transaction marked SUCCESS
     * - Invoice booking status transitions: PENDING_PAYMENT → PAID
     * - Shift closure records MoMo collection
     *
     * Webhook is publicly accessible (no authentication required).
     * MoMo will POST to this endpoint after customer completes payment.
     */
    @PostMapping("/momo")
    @Operation(
            operationId = "10-01-momo-ipn",
            summary = "[WEBHOOK] MoMo IPN async payment notification",
            description = """
                    Receives MoMo sandbox IPN callbacks. 
                    When resultCode = 0, marks payment as SUCCESS and updates booking status from PENDING_PAYMENT to PAID.
                    Idempotent: safe to retry if network failure occurs.
                    """
    )
    @ApiResponse(responseCode = "200", description = "IPN processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or missing orderId")
    @ApiResponse(responseCode = "404", description = "Payment transaction not found")
    public ResponseEntity<Map<String, Object>> handleMomoIpn(@RequestBody WebhookIpnPayload payload) {
        log.info("MoMo IPN received: orderId={}, resultCode={}, transId={}",
                payload.getOrderId(), payload.getResultCode(), payload.getTransId());

        try {
            // InvoiceService handles:
            // 1. Payment transaction state update
            // 2. Booking status transition (if resultCode == 0)
            // 3. Shift closure recording
            invoiceService.handleMomoIpn(payload);

            log.info("MoMo IPN processed successfully: orderId={}", payload.getOrderId());
            return ResponseEntity.ok(Map.of(
                    "resultCode", 0,
                    "message", "IPN processed successfully",
                    "orderId", payload.getOrderId()
            ));
        } catch (Exception ex) {
            log.error("MoMo IPN processing failed: orderId={}, error={}",
                    payload.getOrderId(), ex.getMessage(), ex);
            // Return 200 to MoMo to prevent retry loop, but log error for investigation
            return ResponseEntity.ok(Map.of(
                    "resultCode", 1,
                    "message", "IPN processing error: " + ex.getMessage(),
                    "orderId", payload.getOrderId()
            ));
        }
    }
}