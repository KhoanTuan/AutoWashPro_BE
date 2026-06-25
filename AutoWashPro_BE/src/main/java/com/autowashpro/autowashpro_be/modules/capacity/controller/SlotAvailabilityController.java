package com.autowashpro.autowashpro_be.modules.capacity.controller;

import com.autowashpro.autowashpro_be.modules.capacity.dto.SlotAvailabilityResponse;
import com.autowashpro.autowashpro_be.modules.capacity.service.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/capacity/slots")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "11 - Capacity & Scheduling", description = "Public slot availability and appointment scheduling")
public class SlotAvailabilityController {

    private final SlotAvailabilityService slotAvailabilityService;

    @GetMapping("/availability")
    @Operation(
            operationId = "11-01-slot-availability",
            summary = "[PUBLIC] View open service slot timings",
            description = "Returns each configured slot with remaining capacity for the requested business day. No authentication required."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Slot availability list",
            content = @Content(schema = @Schema(implementation = SlotAvailabilityResponse.class))
    )
    public ResponseEntity<List<SlotAvailabilityResponse>> getAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(slotAvailabilityService.getAvailability(targetDate));
    }
}
