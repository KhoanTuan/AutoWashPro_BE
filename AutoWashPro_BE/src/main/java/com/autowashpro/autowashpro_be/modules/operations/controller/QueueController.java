package com.autowashpro.autowashpro_be.modules.operations.controller;

import com.autowashpro.autowashpro_be.modules.operations.dto.RealtimeQueueDto;
import com.autowashpro.autowashpro_be.modules.operations.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_08_OPERATIONS_QUEUE;

@RestController
@RequestMapping("/api/v1/operations/queue")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_08_OPERATIONS_QUEUE, description = "Shop-floor queue monitor — double-queue layout (appointment + walk-in)")
public class QueueController {

    private final QueueService queueService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_TECH_QUEUE')")
    @Operation(
            operationId = "08-01-queue-layout",
            summary = "[READ] Live shop-floor queue layout",
            description = "Returns the double-queue snapshot ordered by dynamic `priority_score` for technician dashboards."
    )
    @ApiResponse(responseCode = "200", description = "Queue layout retrieved",
            content = @Content(schema = @Schema(implementation = RealtimeQueueDto.class)))
    public ResponseEntity<RealtimeQueueDto> getQueueLayout(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(queueService.getRealtimeQueue(date));
    }

    @GetMapping("/realtime")
    @PreAuthorize("hasAuthority('MONITOR_REALTIME_QUEUE')")
    @Operation(
            operationId = "08-02-realtime-monitor",
            summary = "[READ] Real-time operations monitor",
            description = "Manager-facing monitor with lane summaries, priority scores, and bay occupancy counters."
    )
    @ApiResponse(responseCode = "200", description = "Realtime monitor data",
            content = @Content(schema = @Schema(implementation = RealtimeQueueDto.class)))
    public ResponseEntity<RealtimeQueueDto> monitorRealtime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(queueService.getRealtimeQueue(date));
    }

    @PostMapping("/{bookingId}/check-in")
    @PreAuthorize("hasAuthority('CASHIER_CHECKIN')")
    @Operation(
            operationId = "08-03-check-in",
            summary = "[ACTION] Check-in vehicle to shop-floor queue",
            description = "Records check-in time, finalizes dynamic pricing for revenue collection, and enqueues the vehicle."
    )
    @ApiResponse(responseCode = "200", description = "Vehicle checked in and enqueued")
    public ResponseEntity<RealtimeQueueDto.QueueEntryDto> checkIn(@PathVariable Long bookingId) {
        return ResponseEntity.ok(queueService.checkIn(bookingId));
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAuthority('MONITOR_REALTIME_QUEUE')")
    @Operation(
            operationId = "08-04-recalculate",
            summary = "[ACTION] Recalculate queue priority scores",
            description = "Re-runs the double-queue routing algorithm for all active vehicles on the selected business day."
    )
    @ApiResponse(responseCode = "200", description = "Priorities recalculated",
            content = @Content(schema = @Schema(implementation = RealtimeQueueDto.class)))
    public ResponseEntity<RealtimeQueueDto> recalculate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(queueService.recalculatePriorities(date));
    }
}
