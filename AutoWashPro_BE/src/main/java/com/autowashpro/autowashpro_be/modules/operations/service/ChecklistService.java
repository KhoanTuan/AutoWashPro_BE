package com.autowashpro.autowashpro_be.modules.operations.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.TaskStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.state.BookingStateTransitionValidator;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.modules.operations.dto.TechnicalChecklistDto;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import com.autowashpro.autowashpro_be.modules.operations.entity.TechnicalChecklistItem;
import com.autowashpro.autowashpro_be.modules.operations.repository.TaskChecklistRepository;
import com.autowashpro.autowashpro_be.modules.operations.repository.TechnicalChecklistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private static final List<ChecklistTemplate> DEFAULT_ITEMS = List.of(
            new ChecklistTemplate("PRE_WASH_RINSE", "Pre-wash rinse", 1),
            new ChecklistTemplate("SOAP_APPLICATION", "Soap application", 2),
            new ChecklistTemplate("BRUSH_WASH", "Brush / touchless wash", 3),
            new ChecklistTemplate("RINSE_CYCLE", "Final rinse cycle", 4),
            new ChecklistTemplate("INTERIOR_VACUUM", "Interior vacuum", 5),
            new ChecklistTemplate("TIRE_SHINE", "Tire shine", 6),
            new ChecklistTemplate("QUALITY_INSPECTION", "Quality inspection", 7)
    );

    private final TaskChecklistRepository taskChecklistRepository;
    private final TechnicalChecklistItemRepository checklistItemRepository;
    private final BookingRepository bookingRepository;
    private final StaffRepository staffRepository;
    private final OperationsMapper operationsMapper;

    @Transactional
    public TechnicalChecklistDto initializeForBooking(Booking booking, Staff technician) {
        TaskChecklist task = taskChecklistRepository.findFirstByBookingBookingIdOrderByIdAsc(booking.getBookingId())
                .orElseGet(() -> TaskChecklist.builder()
                        .booking(booking)
                        .technician(technician)
                        .status(TaskStatus.NOT_STARTED)
                        .build());

        if (!task.getTechnician().getStaffId().equals(technician.getStaffId())) {
            task.setTechnician(technician);
        }

        if (task.getItems().isEmpty()) {
            initializeChecklistItems(task);
        }

        taskChecklistRepository.save(task);
        return operationsMapper.toTechnicalChecklistDto(hydrate(task));
    }

    @Transactional(readOnly = true)
    public TechnicalChecklistDto getByBookingId(Long bookingId) {
        TaskChecklist task = taskChecklistRepository.findFirstByBookingBookingIdOrderByIdAsc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Task checklist not found for booking"));
        return operationsMapper.toTechnicalChecklistDto(hydrate(task));
    }

    @Transactional
    public TechnicalChecklistDto claimTask(Long bookingId, Long technicianId) {
        Booking booking = findBooking(bookingId);
        ensurePaid(booking);

        Staff technician = staffRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        if (technician.getStatus() != StaffStatus.ACTIVE) {
            throw new BadRequestException("Technician is not active");
        }

        TaskChecklist task = taskChecklistRepository.findFirstByBookingBookingIdOrderByIdAsc(bookingId)
                .orElseGet(() -> taskChecklistRepository.save(TaskChecklist.builder()
                        .booking(booking)
                        .technician(technician)
                        .status(TaskStatus.NOT_STARTED)
                        .build()));

        if (!task.getTechnician().getStaffId().equals(technicianId)) {
            throw new BadRequestException("Task already claimed by another technician");
        }

        if (task.getItems().isEmpty()) {
            initializeChecklistItems(task);
        }

        if (task.getStatus() == TaskStatus.NOT_STARTED) {
            task.setStatus(TaskStatus.PROCESSING);
            task.setStartTime(LocalDateTime.now());
            technician.setWorkStatus(StaffWorkStatus.BUSY);
            staffRepository.save(technician);
            BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.PROCESSING);
            booking.setBookingStatus(BookingStatus.PROCESSING);
            bookingRepository.save(booking);
        }

        taskChecklistRepository.save(task);
        return operationsMapper.toTechnicalChecklistDto(hydrate(task));
    }

    @Transactional
    public TechnicalChecklistDto completeChecklistItem(Long itemId) {
        TechnicalChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));
        if (Boolean.TRUE.equals(item.getCompleted())) {
            return operationsMapper.toTechnicalChecklistDto(hydrate(item.getTaskChecklist()));
        }

        item.setCompleted(true);
        item.setCompletedAt(LocalDateTime.now());
        checklistItemRepository.save(item);
        return operationsMapper.toTechnicalChecklistDto(hydrate(item.getTaskChecklist()));
    }

    @Transactional
    public TechnicalChecklistDto completeService(Long bookingId) {
        TaskChecklist task = taskChecklistRepository.findFirstByBookingBookingIdOrderByIdAsc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Task checklist not found for booking"));

        boolean allCompleted = task.getItems().stream().allMatch(i -> Boolean.TRUE.equals(i.getCompleted()));
        if (!allCompleted) {
            throw new BadRequestException("All checklist items must be completed before finishing the service");
        }

        task.setStatus(TaskStatus.DONE);
        task.setEndTime(LocalDateTime.now());

        Staff technician = task.getTechnician();
        technician.setWorkStatus(StaffWorkStatus.IDLE);
        technician.setTotalJobsCompleted(
                (technician.getTotalJobsCompleted() != null ? technician.getTotalJobsCompleted() : 0) + 1
        );
        staffRepository.save(technician);

        Booking booking = task.getBooking();
        BookingStateTransitionValidator.validateStateTransition(booking.getBookingStatus(), BookingStatus.COMPLETED);
        booking.setBookingStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        taskChecklistRepository.save(task);
        return operationsMapper.toTechnicalChecklistDto(hydrate(task));
    }

    @Transactional(readOnly = true)
    public List<TechnicalChecklistDto> listActiveForTechnician(Long technicianId) {
        return taskChecklistRepository
                .findByTechnicianStaffIdAndStatusInOrderByCreatedAtAsc(
                        technicianId,
                        List.of(TaskStatus.NOT_STARTED, TaskStatus.PROCESSING)
                )
                .stream()
                .map(this::hydrate)
                .map(operationsMapper::toTechnicalChecklistDto)
                .toList();
    }

    private void initializeChecklistItems(TaskChecklist task) {
        for (ChecklistTemplate template : DEFAULT_ITEMS) {
            task.getItems().add(TechnicalChecklistItem.builder()
                    .taskChecklist(task)
                    .itemCode(template.code())
                    .itemLabel(template.label())
                    .sortOrder(template.sortOrder())
                    .completed(false)
                    .build());
        }
    }

    private TaskChecklist hydrate(TaskChecklist task) {
        task.getBooking().getBookingCode();
        task.getBooking().getCustomer().getFullName();
        task.getBooking().getVehicle().getLicensePlate();
        task.getBooking().getBookingItems().forEach(i -> i.getVariant().getService().getServiceName());
        task.getTechnician().getFullName();
        task.getItems().size();
        return task;
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void ensurePaid(Booking booking) {
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Payment required before claiming tasks");
        }
    }

    private record ChecklistTemplate(String code, String label, int sortOrder) {
    }
}
