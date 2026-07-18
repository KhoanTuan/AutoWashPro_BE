package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

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

        boolean isDefault = request.getIsDefault() != null ? request.getIsDefault() : false;
        if (currentVehicleCount == 0) {
            isDefault = true;
        } else if (isDefault) {
            List<Vehicle> allVehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId);
            for (Vehicle v : allVehicles) {
                v.setIsDefault(false);
            }
            vehicleRepository.saveAll(allVehicles);
        }

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .licensePlate(cleanPlate)
                .model(request.getModel() != null ? request.getModel().trim() : null)
                .isDefault(isDefault)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Added vehicle {} for customer {}", cleanPlate, customerId);
        return mapToResponse(saved);
    }

    @Transactional
    public void setDefaultVehicle(Long customerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (!vehicle.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa xe này!");
        }

        List<Vehicle> allVehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId);
        for (Vehicle v : allVehicles) {
            v.setIsDefault(v.getVehicleId().equals(vehicleId));
        }
        vehicleRepository.saveAll(allVehicles);
        log.info("Set vehicle {} as default for customer {}", vehicleId, customerId);
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

        if (request.getIsDefault() != null && request.getIsDefault()) {
            List<Vehicle> allVehicles = vehicleRepository.findByCustomerCustomerIdOrderByCreatedAtAsc(customerId);
            for (Vehicle v : allVehicles) {
                if (!v.getVehicleId().equals(vehicleId)) {
                    v.setIsDefault(false);
                }
            }
            vehicleRepository.saveAll(allVehicles);
            vehicle.setIsDefault(true);
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Updated vehicle {} (ID: {})", cleanPlate, vehicleId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteVehicle(Long customerId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (!vehicle.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền xóa xe này!");
        }

        vehicleRepository.delete(vehicle);
        log.info("Deleted vehicle ID: {} (Plate: {})", vehicleId, vehicle.getLicensePlate());
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
