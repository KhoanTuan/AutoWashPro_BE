package com.autowashpro.autowashpro_be.modules.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Real-time shop-floor queue snapshot for monitor dashboards")
public class RealtimeQueueDto {

    @Schema(description = "Appointment lane — customers with scheduled slots")
    private List<QueueEntryDto> appointmentLane;

    @Schema(description = "Walk-in lane — customers without prior appointment")
    private List<QueueEntryDto> walkInLane;

    @Schema(description = "Aggregate queue metrics for the current business day")
    private QueueSummaryDto summary;

    @Data
    @Builder
    @Schema(description = "Single vehicle waiting in a queue lane")
    public static class QueueEntryDto {
        private Long queueId;
        private Long bookingId;
        private String bookingCode;
        private String customerName;
        private String licensePlate;
        private String serviceName;
        private String tierName;
        private String slotLabel;
        private String bookingType;
        private String queueLane;
        private String queueStatus;
        private Double priorityScore;
        private Integer lanePosition;
        private LocalDateTime checkInTime;
        private BigDecimal finalizedTotalPrice;
        private Long technicianId;
        private String technicianName;
    }

    @Data
    @Builder
    @Schema(description = "Queue counters for operations monitoring")
    public static class QueueSummaryDto {
        private int appointmentWaiting;
        private int walkInWaiting;
        private int inBay;
        private int completedToday;
    }
}
