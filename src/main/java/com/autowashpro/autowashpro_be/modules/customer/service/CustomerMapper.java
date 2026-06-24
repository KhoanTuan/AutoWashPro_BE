package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.modules.customer.dto.CustomerOptionResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.CustomerResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerMapper {

    private final VehicleRepository vehicleRepository;

    public CustomerResponse toResponse(Customer customer) {
        Vehicle primary = vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(customer.getCustomerId())
                .orElse(null);

        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .fullName(customer.getFullName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .licensePlate(primary != null ? primary.getLicensePlate() : null)
                .carType(primary != null ? primary.getCarType().name() : null)
                .status(customer.getStatus().name())
                .statusLabel(statusLabel(customer.getStatus()))
                .tierName(customer.getTier() != null ? customer.getTier().getTierName() : null)
                .visitCount(customer.getVisitCount())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalSpending(customer.getTotalSpending())
                .lastCompletedBookingAt(customer.getLastCompletedBookingAt())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    public CustomerOptionResponse toOption(Customer customer) {
        Vehicle primary = vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(customer.getCustomerId())
                .orElse(null);

        return CustomerOptionResponse.builder()
                .customerId(customer.getCustomerId())
                .fullName(customer.getFullName())
                .phoneNumber(customer.getPhoneNumber())
                .licensePlate(primary != null ? primary.getLicensePlate() : null)
                .tierName(customer.getTier() != null ? customer.getTier().getTierName() : null)
                .build();
    }

    public String statusLabel(CustomerStatus status) {
        return switch (status) {
            case INACTIVE -> "Inactive";
            case PENDING_ACTIVATION -> "Pending";
            default -> "Active";
        };
    }

    public CarType parseCarType(String carType) {
        if (carType == null || carType.isBlank()) {
            return CarType.SEDAN;
        }
        return CarType.valueOf(carType.toUpperCase());
    }
}
