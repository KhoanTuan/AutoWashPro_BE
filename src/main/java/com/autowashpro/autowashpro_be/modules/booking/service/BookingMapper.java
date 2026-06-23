package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotOptionResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceVariantRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.SlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.WashServiceRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffWorkStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class BookingMapper {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final ServiceVariantRepository serviceVariantRepository;

    public BookingMapper(ServiceVariantRepository serviceVariantRepository) {
        this.serviceVariantRepository = serviceVariantRepository;
    }

    public BookingResponse toResponse(Booking booking) {
        TaskChecklist task = booking.getTaskChecklists().isEmpty() ? null : booking.getTaskChecklists().get(0);
        BookingItem item = booking.getBookingItems().isEmpty() ? null : booking.getBookingItems().get(0);

        BigDecimal total = booking.getBookingItems().stream()
                .map(BookingItem::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String tierName = booking.getCustomer().getTier() != null
                ? booking.getCustomer().getTier().getTierName()
                : "REGULAR";

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer().getPhoneNumber())
                .membership(formatTier(tierName))
                .plate(booking.getVehicle().getLicensePlate())
                .slotLabel(formatSlot(booking.getSlot()))
                .bookingType(booking.getBookingType().name())
                .bookingTypeLabel(booking.getBookingType() == BookingType.WALKIN ? "Walk-in" : "Appt")
                .serviceName(item != null ? item.getVariant().getService().getServiceName() : "—")
                .totalAmount(total)
                .bookingStatus(booking.getBookingStatus().name())
                .bookingStatusLabel(statusLabel(booking.getBookingStatus()))
                .paymentStatus(booking.getPaymentStatus().name())
                .paymentStatusLabel(booking.getPaymentStatus() == PaymentStatus.PAID ? "Paid" : "Unpaid")
                .technicianId(task != null ? task.getTechnician().getStaffId() : null)
                .technicianName(task != null ? task.getTechnician().getFullName() : null)
                .notes(booking.getNotes())
                .bookingDate(booking.getBookingDate())
                .createdAt(booking.getCreatedAt())
                .action(resolveAction(booking, task))
                .build();
    }

    public ServiceCatalogResponse toCatalog(WashService service) {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (CarType carType : CarType.values()) {
            serviceVariantRepository.findByServiceServiceIdAndCarType(service.getServiceId(), carType)
                    .ifPresent(v -> prices.put(carType.name(), v.getCalculatedPrice()));
        }
        return ServiceCatalogResponse.builder()
                .serviceId(service.getServiceId())
                .name(service.getServiceName())
                .duration(service.getDurationMinutes() + " min")
                .basePrice(service.getBasePrice())
                .pricesByCarType(prices)
                .build();
    }

    public SlotOptionResponse toSlotOption(Slot slot) {
        return SlotOptionResponse.builder()
                .slotId(slot.getSlotId())
                .label(formatSlot(slot))
                .startTime(slot.getStartTime().toString())
                .endTime(slot.getEndTime().toString())
                .build();
    }

    public String formatSlot(Slot slot) {
        return TIME_FMT.format(slot.getStartTime()) + " - " + TIME_FMT.format(slot.getEndTime());
    }

    public String statusLabel(BookingStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Pending Payment";
            case CONFIRMED -> "Confirmed";
            case PROCESSING -> "Processing";
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
        };
    }

    private String formatTier(String tierName) {
        return switch (tierName) {
            case "GOLD", "PLATINUM", "SILVER" -> tierName.charAt(0) + tierName.substring(1).toLowerCase() + " Member";
            default -> "Regular";
        };
    }

    private String resolveAction(Booking booking, TaskChecklist task) {
        if (booking.getBookingStatus() == BookingStatus.CANCELED
                || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            return null;
        }
        if (booking.getPaymentStatus() == PaymentStatus.UNPAID) {
            return "Checkout";
        }
        if (task == null) {
            return "Assign";
        }
        return switch (booking.getBookingStatus()) {
            case PENDING_PAYMENT -> "Accept";
            case CONFIRMED -> "Start";
            case PROCESSING -> "Complete";
            default -> null;
        };
    }
}
