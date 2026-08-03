package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId);
        }
        return vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public VehicleResponse addVehicle(Long customerId, VehicleRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));

        int maxVehicles = (customer.getTier() != null && customer.getTier().getTierName() != null) ? switch (customer.getTier().getTierName().toUpperCase()) {
            case "SILVER" -> 5;
            case "GOLD" -> 10;
            case "PLATINUM" -> 20;
            default -> 3;
        } : 3;

        long currentVehicleCount = vehicleRepository.countByCustomerCustomerId(customerId);
        if (currentVehicleCount >= maxVehicles) {
            throw new BadRequestException("Garage của bạn đã đạt giới hạn số lượng xe tối đa (" + maxVehicles + " xe) dành cho hạng " + 
                    (customer.getTier() != null ? customer.getTier().getTierName() : "REGULAR") + ". Vui lòng nâng hạng để liên hệ và thêm nhiều xe hơn!");
        }

        String cleanPlate = request.getLicensePlate().trim().toUpperCase();

        if (vehicleRepository.existsByLicensePlateIgnoreCase(cleanPlate)) {
            throw new BadRequestException("Biển số xe '" + cleanPlate + "' đã được đăng ký trong hệ thống!");
        }

        boolean isFirstVehicle = currentVehicleCount == 0;
        boolean shouldBeDefault = isFirstVehicle || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && currentVehicleCount > 0) {
            List<Vehicle> existingVehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId);
            for (Vehicle v : existingVehicles) {
                v.setIsDefault(false);
            }
            vehicleRepository.saveAll(existingVehicles);
        }

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .licensePlate(cleanPlate)
                .model(request.getModel() != null ? request.getModel().trim() : null)
                .isDefault(shouldBeDefault)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Added vehicle {} for customer {} (isDefault: {})", cleanPlate, customerId, shouldBeDefault);
        return mapToResponse(saved);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long customerId, Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (!vehicle.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa thông tin xe này!");
        }

        String cleanPlate = request.getLicensePlate().trim().toUpperCase();
        if (!vehicle.getLicensePlate().equalsIgnoreCase(cleanPlate)) {
            if (vehicleRepository.existsByLicensePlateIgnoreCase(cleanPlate)) {
                throw new BadRequestException("Biển số xe '" + cleanPlate + "' đã được đăng ký trong hệ thống!");
            }
            vehicle.setLicensePlate(cleanPlate);
        }

        if (request.getModel() != null) {
            vehicle.setModel(request.getModel().trim());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Updated vehicle {} (ID: {})", cleanPlate, vehicleId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteVehicle(Long customerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (customerId != null && !vehicle.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền xóa xe này!");
        }

        validateVehicleDeletion(vehicle);

        vehicleRepository.delete(vehicle);
        log.info("Deleted vehicle ID: {} (Plate: {})", vehicleId, vehicle.getLicensePlate());
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        deleteVehicle(null, vehicleId);
    }

    private void validateVehicleDeletion(Vehicle vehicle) {
        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().isBlank()) {
            return;
        }

        List<Booking> associatedBookings = bookingRepository.findByLicensePlateIgnoreCase(vehicle.getLicensePlate());
        if (associatedBookings == null || associatedBookings.isEmpty()) {
            return;
        }

        // Rule 1: Check for PENDING status
        boolean hasPendingBooking = associatedBookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.PENDING);
        if (hasPendingBooking) {
            throw new BadRequestException("Không thể xóa xe này do xe đang có lịch đặt ở trạng thái chờ xử lý (Pending). Vui lòng hủy lịch đặt trước khi xóa xe.");
        }

        // Rule 2: Check for paid/confirmed/active bookings scheduled in the future
        LocalDateTime now = LocalDateTime.now();
        boolean hasFutureActiveBooking = associatedBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED_BY_CUSTOMER && b.getStatus() != BookingStatus.CANCELLED_NO_SHOW)
                .anyMatch(b -> {
                    if (b.getBookingDate() == null) {
                        return false;
                    }
                    LocalDateTime scheduledTime;
                    if (b.getTimeSlot() != null && b.getTimeSlot().getStartTime() != null) {
                        scheduledTime = LocalDateTime.of(b.getBookingDate(), b.getTimeSlot().getStartTime());
                    } else {
                        scheduledTime = b.getBookingDate().atStartOfDay();
                    }
                    return scheduledTime.isAfter(now);
                });

        if (hasFutureActiveBooking) {
            throw new BadRequestException("Không thể xóa xe này vì xe đang có lịch đặt dịch vụ chưa hoàn thành trong tương lai.");
        }
    }

    @Transactional
    public VehicleResponse setDefaultVehicle(Long customerId, Long vehicleId) {
        Vehicle targetVehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (!targetVehicle.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa xe này!");
        }

        List<Vehicle> customerVehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId);
        for (Vehicle v : customerVehicles) {
            v.setIsDefault(v.getVehicleId().equals(vehicleId));
        }
        vehicleRepository.saveAll(customerVehicles);
        
        log.info("Set vehicle {} (ID: {}) as default for customer {}", targetVehicle.getLicensePlate(), vehicleId, customerId);
        return mapToResponse(targetVehicle);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .customerId(vehicle.getCustomer().getCustomerId())
                .licensePlate(vehicle.getLicensePlate())
                .model(vehicle.getModel())
                .createdAt(vehicle.getCreatedAt())
                .isDefault(vehicle.getIsDefault())
                .build();
    }
}
