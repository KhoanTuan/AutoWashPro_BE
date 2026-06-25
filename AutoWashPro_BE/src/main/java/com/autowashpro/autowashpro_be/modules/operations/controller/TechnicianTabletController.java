package com.autowashpro.autowashpro_be.modules.operations.controller;

import com.autowashpro.autowashpro_be.modules.operations.dto.ClaimTaskRequest;
import com.autowashpro.autowashpro_be.modules.operations.dto.TechnicalChecklistDto;
import com.autowashpro.autowashpro_be.modules.operations.service.ChecklistService;
import com.autowashpro.autowashpro_be.modules.operations.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_09_TECHNICIAN_TABLET;

@RestController
@RequestMapping("/api/v1/operations/technician")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_09_TECHNICIAN_TABLET, description = "Technician tablet API — claim bays, view queue, complete checklists")
public class TechnicianTabletController {

    private final ChecklistService checklistService;
    private final QueueService queueService;

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('VIEW_TECH_QUEUE')")
    @Operation(
            operationId = "09-01-tech-queue",
            summary = "[READ] Technician task queue",
            description = "Lists active service sessions assigned to a technician, optimized for tablet bay interfaces."
    )
    @ApiResponse(responseCode = "200", description = "Technician queue retrieved")
    public ResponseEntity<List<TechnicalChecklistDto>> technicianQueue(
            @RequestParam Long technicianId
    ) {
        return ResponseEntity.ok(checklistService.listActiveForTechnician(technicianId));
    }

    @PostMapping("/tasks/{bookingId}/claim")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(
            operationId = "09-02-claim-task",
            summary = "[ACTION] Claim vehicle from queue",
            description = "Technician claims a waiting vehicle, initializes the technical checklist, and moves to PROCESSING."
    )
    @ApiResponse(responseCode = "200", description = "Task claimed",
            content = @Content(schema = @Schema(implementation = TechnicalChecklistDto.class)))
    public ResponseEntity<TechnicalChecklistDto> claimTask(
            @PathVariable Long bookingId,
            @Valid @RequestBody ClaimTaskRequest request
    ) {
        TechnicalChecklistDto checklist = checklistService.claimTask(bookingId, request.getTechnicianId());
        queueService.markQueueClaimed(bookingId);
        queueService.markQueueInBay(bookingId);
        return ResponseEntity.ok(checklist);
    }

    @GetMapping("/tasks/{bookingId}/checklist")
    @PreAuthorize("hasAuthority('VIEW_TECH_QUEUE')")
    @Operation(
            operationId = "09-03-view-checklist",
            summary = "[READ] View technical checklist for a session",
            description = "Returns checklist items and completion progress for the technician tablet UI."
    )
    @ApiResponse(responseCode = "200", description = "Checklist retrieved",
            content = @Content(schema = @Schema(implementation = TechnicalChecklistDto.class)))
    public ResponseEntity<TechnicalChecklistDto> getChecklist(@PathVariable Long bookingId) {
        return ResponseEntity.ok(checklistService.getByBookingId(bookingId));
    }

    @PatchMapping("/checklist-items/{itemId}/complete")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(
            operationId = "09-04-complete-item",
            summary = "[ACTION] Mark checklist item complete",
            description = "Technician checks off an individual technical step during the wash session."
    )
    @ApiResponse(responseCode = "200", description = "Item marked complete",
            content = @Content(schema = @Schema(implementation = TechnicalChecklistDto.class)))
    public ResponseEntity<TechnicalChecklistDto> completeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(checklistService.completeChecklistItem(itemId));
    }

    @PatchMapping("/tasks/{bookingId}/complete")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(
            operationId = "09-05-complete-service",
            summary = "[ACTION] Complete service session",
            description = "Validates all checklist items are done, completes the booking, and releases the technician."
    )
    @ApiResponse(responseCode = "200", description = "Service completed",
            content = @Content(schema = @Schema(implementation = TechnicalChecklistDto.class)))
    public ResponseEntity<TechnicalChecklistDto> completeService(@PathVariable Long bookingId) {
        TechnicalChecklistDto checklist = checklistService.completeService(bookingId);
        queueService.markQueueCompleted(bookingId);
        return ResponseEntity.ok(checklist);
    }
}
