package com.autowashpro.autowashpro_be.modules.operations.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingItem;
import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.operations.dto.DynamicPricingResultDto;
import com.autowashpro.autowashpro_be.modules.operations.dto.RealtimeQueueDto;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueLane;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueStatus;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import com.autowashpro.autowashpro_be.modules.operations.entity.WaitingQueue;
import com.autowashpro.autowashpro_be.modules.operations.repository.TaskChecklistRepository;
import com.autowashpro.autowashpro_be.modules.operations.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final List<QueueStatus> ACTIVE_STATUSES = List.of(
            QueueStatus.WAITING,
            QueueStatus.CLAIMED,
            QueueStatus.IN_BAY
    );

    private final WaitingQueueRepository waitingQueueRepository;
    private final TaskChecklistRepository taskChecklistRepository;
    private final BookingRepository bookingRepository;
    private final QueueRoutingEngine queueRoutingEngine;
    private final DynamicPricingEngine dynamicPricingEngine;
    private final OperationsMapper operationsMapper;

    @Transactional(readOnly = true)
    public RealtimeQueueDto getRealtimeQueue(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<WaitingQueue> appointmentLane = waitingQueueRepository
                .findByQueueLaneAndQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(
                        QueueLane.APPOINTMENT,
                        ACTIVE_STATUSES,
                        targetDate
                );
        List<WaitingQueue> walkInLane = waitingQueueRepository
                .findByQueueLaneAndQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(
                        QueueLane.WALK_IN,
                        ACTIVE_STATUSES,
                        targetDate
                );

        Map<Long, TaskChecklist> tasksByBooking = taskChecklistRepository.findAll().stream()
                .collect(Collectors.toMap(t -> t.getBooking().getBookingId(), Function.identity(), (a, b) -> a));

        return RealtimeQueueDto.builder()
                .appointmentLane(mapLane(appointmentLane, tasksByBooking))
                .walkInLane(mapLane(walkInLane, tasksByBooking))
                .summary(operationsMapper.toSummary(appointmentLane, walkInLane))
                .build();
    }

    @Transactional
    public RealtimeQueueDto.QueueEntryDto checkIn(Long bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Booking must be paid before shop-floor check-in");
        }

        LocalDateTime checkInTime = LocalDateTime.now();
        booking.setCheckInTime(checkInTime);
        applyFinalizedPricing(booking);
        bookingRepository.save(booking);

        WaitingQueue queue = waitingQueueRepository.findByBookingBookingId(bookingId)
                .orElseGet(() -> WaitingQueue.builder().booking(booking).build());

        queue.setQueueLane(queueRoutingEngine.resolveLane(booking));
        queue.setQueueStatus(QueueStatus.WAITING);
        queue.setCheckInTime(checkInTime);
        queue.setPriorityScore(queueRoutingEngine.computePriorityScore(booking, checkInTime));

        waitingQueueRepository.save(queue);
        recalculateLanePositions(queue.getQueueLane(), booking.getBookingDate());

        TaskChecklist task = taskChecklistRepository.findFirstByBookingBookingIdOrderByIdAsc(bookingId).orElse(null);
        return operationsMapper.toQueueEntry(
                waitingQueueRepository.findByBookingBookingId(bookingId).orElse(queue),
                task
        );
    }

    @Transactional
    public RealtimeQueueDto recalculatePriorities(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<WaitingQueue> activeQueues = waitingQueueRepository
                .findByQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(ACTIVE_STATUSES, targetDate);

        for (WaitingQueue queue : activeQueues) {
            Booking booking = queue.getBooking();
            LocalDateTime checkInTime = queue.getCheckInTime() != null ? queue.getCheckInTime() : LocalDateTime.now();
            queue.setPriorityScore(queueRoutingEngine.computePriorityScore(booking, checkInTime));
        }
        waitingQueueRepository.saveAll(activeQueues);

        recalculateLanePositions(QueueLane.APPOINTMENT, targetDate);
        recalculateLanePositions(QueueLane.WALK_IN, targetDate);

        return getRealtimeQueue(targetDate);
    }

    @Transactional
    public void markQueueClaimed(Long bookingId) {
        WaitingQueue queue = waitingQueueRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));
        queue.setQueueStatus(QueueStatus.CLAIMED);
        waitingQueueRepository.save(queue);
    }

    @Transactional
    public void markQueueInBay(Long bookingId) {
        WaitingQueue queue = waitingQueueRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));
        queue.setQueueStatus(QueueStatus.IN_BAY);
        waitingQueueRepository.save(queue);
    }

    @Transactional
    public void markQueueCompleted(Long bookingId) {
        waitingQueueRepository.findByBookingBookingId(bookingId).ifPresent(queue -> {
            queue.setQueueStatus(QueueStatus.COMPLETED);
            waitingQueueRepository.save(queue);
        });
    }

    @Transactional
    public DynamicPricingResultDto previewPricing(Long bookingId) {
        Booking booking = findBooking(bookingId);
        return applyFinalizedPricing(booking);
    }

    private DynamicPricingResultDto applyFinalizedPricing(Booking booking) {
        BookingItem item = booking.getBookingItems().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Booking has no service items"));

        String serviceName = item.getVariant().getService().getServiceName();
        var servicePackage = dynamicPricingEngine.resolvePackageFromServiceName(serviceName);
        DynamicPricingResultDto pricing = dynamicPricingEngine.calculate(
                servicePackage,
                booking.getVehicle().getCarType()
        );

        booking.finalizeRevenue(pricing.getBasePrice(), pricing.getSurchargeAmount(), pricing.getFinalizedTotalPrice());
        item.setActualPrice(pricing.getFinalizedTotalPrice());
        return pricing;
    }

    private List<RealtimeQueueDto.QueueEntryDto> mapLane(
            List<WaitingQueue> lane,
            Map<Long, TaskChecklist> tasksByBooking
    ) {
        return lane.stream()
                .sorted(Comparator.comparing(WaitingQueue::getLanePosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(WaitingQueue::getPriorityScore, Comparator.reverseOrder()))
                .map(queue -> {
                    queue.getBooking().getCustomer().getFullName();
                    TaskChecklist task = tasksByBooking.get(queue.getBooking().getBookingId());
                    return operationsMapper.toQueueEntry(queue, task);
                })
                .toList();
    }

    private void recalculateLanePositions(QueueLane lane, LocalDate date) {
        List<WaitingQueue> laneQueues = waitingQueueRepository
                .findByQueueLaneAndQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(lane, ACTIVE_STATUSES, date);
        laneQueues.sort(Comparator.comparing(WaitingQueue::getPriorityScore).reversed());

        int position = 1;
        for (WaitingQueue queue : laneQueues) {
            queue.setLanePosition(position++);
        }
        waitingQueueRepository.saveAll(laneQueues);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }
}
