package com.autowashpro.autowashpro_be.modules.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Technician tablet view of a service session checklist")
public class TechnicalChecklistDto {

    private Long taskChecklistId;
    private Long bookingId;
    private String bookingCode;
    private String customerName;
    private String licensePlate;
    private String serviceName;
    private String taskStatus;
    private Long technicianId;
    private String technicianName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int completedItems;
    private int totalItems;
    private boolean allItemsCompleted;
    private List<ChecklistItemDto> items;

    @Data
    @Builder
    @Schema(description = "Individual technical checklist step")
    public static class ChecklistItemDto {
        private Long itemId;
        private String itemCode;
        private String itemLabel;
        private Integer sortOrder;
        private boolean completed;
        private LocalDateTime completedAt;
    }
}
