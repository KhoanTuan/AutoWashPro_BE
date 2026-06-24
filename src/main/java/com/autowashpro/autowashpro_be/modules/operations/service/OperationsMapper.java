package com.autowashpro.autowashpro_be.modules.operations.service;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingItem;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingMapper;
import com.autowashpro.autowashpro_be.modules.operations.dto.RealtimeQueueDto;
import com.autowashpro.autowashpro_be.modules.operations.dto.TechnicalChecklistDto;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueLane;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueStatus;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import com.autowashpro.autowashpro_be.modules.operations.entity.TechnicalChecklistItem;
import com.autowashpro.autowashpro_be.modules.operations.entity.WaitingQueue;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OperationsMapper {

    private final BookingMapper bookingMapper;

    public OperationsMapper(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }

    public RealtimeQueueDto.QueueEntryDto toQueueEntry(WaitingQueue queue, TaskChecklist task) {
        Booking booking = queue.getBooking();
        String serviceName = booking.getBookingItems().isEmpty()
                ? "—"
                : booking.getBookingItems().get(0).getVariant().getService().getServiceName();
        String tierName = booking.getCustomer().getTier() != null
                ? booking.getCustomer().getTier().getTierName()
                : "REGULAR";

        return RealtimeQueueDto.QueueEntryDto.builder()
                .queueId(queue.getQueueId())
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomer().getFullName())
                .licensePlate(booking.getVehicle().getLicensePlate())
                .serviceName(serviceName)
                .tierName(tierName)
                .slotLabel(bookingMapper.formatSlot(booking.getSlot()))
                .bookingType(booking.getBookingType().name())
                .queueLane(queue.getQueueLane().name())
                .queueStatus(queue.getQueueStatus().name())
                .priorityScore(queue.getPriorityScore())
                .lanePosition(queue.getLanePosition())
                .checkInTime(queue.getCheckInTime())
                .finalizedTotalPrice(booking.getFinalizedTotalPrice())
                .technicianId(task != null ? task.getTechnician().getStaffId() : null)
                .technicianName(task != null ? task.getTechnician().getFullName() : null)
                .build();
    }

    public TechnicalChecklistDto toTechnicalChecklistDto(TaskChecklist task) {
        Booking booking = task.getBooking();
        BookingItem item = booking.getBookingItems().isEmpty() ? null : booking.getBookingItems().get(0);
        List<TechnicalChecklistDto.ChecklistItemDto> items = task.getItems().stream()
                .map(this::toChecklistItemDto)
                .toList();
        long completed = items.stream().filter(TechnicalChecklistDto.ChecklistItemDto::isCompleted).count();

        return TechnicalChecklistDto.builder()
                .taskChecklistId(task.getId())
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomer().getFullName())
                .licensePlate(booking.getVehicle().getLicensePlate())
                .serviceName(item != null ? item.getVariant().getService().getServiceName() : "—")
                .taskStatus(task.getStatus().name())
                .technicianId(task.getTechnician().getStaffId())
                .technicianName(task.getTechnician().getFullName())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .completedItems((int) completed)
                .totalItems(items.size())
                .allItemsCompleted(!items.isEmpty() && completed == items.size())
                .items(items)
                .build();
    }

    public RealtimeQueueDto.QueueSummaryDto toSummary(
            List<WaitingQueue> appointmentLane,
            List<WaitingQueue> walkInLane
    ) {
        int waitingAppointment = countByStatus(appointmentLane, QueueStatus.WAITING);
        int waitingWalkIn = countByStatus(walkInLane, QueueStatus.WAITING);
        int inBay = countByStatus(appointmentLane, QueueStatus.IN_BAY)
                + countByStatus(walkInLane, QueueStatus.IN_BAY)
                + countByStatus(appointmentLane, QueueStatus.CLAIMED)
                + countByStatus(walkInLane, QueueStatus.CLAIMED);
        int completed = countByStatus(appointmentLane, QueueStatus.COMPLETED)
                + countByStatus(walkInLane, QueueStatus.COMPLETED);

        return RealtimeQueueDto.QueueSummaryDto.builder()
                .appointmentWaiting(waitingAppointment)
                .walkInWaiting(waitingWalkIn)
                .inBay(inBay)
                .completedToday(completed)
                .build();
    }

    private TechnicalChecklistDto.ChecklistItemDto toChecklistItemDto(TechnicalChecklistItem item) {
        return TechnicalChecklistDto.ChecklistItemDto.builder()
                .itemId(item.getItemId())
                .itemCode(item.getItemCode())
                .itemLabel(item.getItemLabel())
                .sortOrder(item.getSortOrder())
                .completed(Boolean.TRUE.equals(item.getCompleted()))
                .completedAt(item.getCompletedAt())
                .build();
    }

    private int countByStatus(List<WaitingQueue> queues, QueueStatus status) {
        return (int) queues.stream().filter(q -> q.getQueueStatus() == status).count();
    }
}
