package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceCatalog;
import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceType;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingItemRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.ServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final BookingItemRepository bookingItemRepository;

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAllServices(boolean activeOnly) {
        List<ServiceCatalog> catalog = activeOnly
                ? serviceCatalogRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                : serviceCatalogRepository.findAllByOrderByDisplayOrderAsc();
        return catalog.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private ServiceCatalog getOrSaveService(String code, String name, ServiceType type, java.math.BigDecimal price, int duration, String desc, int order) {
        return serviceCatalogRepository.findByServiceCode(code).map(s -> {
            s.setServiceName(name);
            s.setServiceType(type);
            s.setPrice(price);
            s.setDurationMinutes(duration);
            s.setDescription(desc);
            s.setDisplayOrder(order);
            return serviceCatalogRepository.save(s);
        }).orElseGet(() -> {
            ServiceCatalog s = ServiceCatalog.builder()
                    .serviceCode(code)
                    .serviceName(name)
                    .serviceType(type)
                    .price(price)
                    .durationMinutes(duration)
                    .description(desc)
                    .isActive(true)
                    .displayOrder(order)
                    .build();
            return serviceCatalogRepository.save(s);
        });
    }

    @Transactional
    public ServiceCatalogResponse createService(ServiceCatalogRequest request) {
        if (serviceCatalogRepository.existsByServiceCode(request.getServiceCode())) {
            throw new BadRequestException("Service code '" + request.getServiceCode() + "' already exists");
        }

        List<ServiceCatalog> included = new java.util.ArrayList<>();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 15;

        if (request.getServiceType() == ServiceType.PACKAGE) {
            if (request.getIncludedServiceIds() == null || request.getIncludedServiceIds().isEmpty()) {
                throw new BadRequestException("Gói Combo bắt buộc phải chọn ít nhất 1 dịch vụ con!");
            }
            included = serviceCatalogRepository.findAllById(request.getIncludedServiceIds());
            boolean hasNestedPackage = included.stream().anyMatch(s -> s.getServiceType() == ServiceType.PACKAGE);
            if (hasNestedPackage) {
                throw new BadRequestException("Gói Combo chỉ được bao gồm các dịch vụ đơn lẻ, không được chứa gói Combo khác!");
            }

            int sumDuration = included.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
            if ((request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) && sumDuration > 0) {
                duration = sumDuration;
            }
        }

        ServiceCatalog service = ServiceCatalog.builder()
                .serviceCode(request.getServiceCode())
                .serviceName(request.getServiceName())
                .serviceType(request.getServiceType())
                .price(request.getPrice())
                .durationMinutes(duration)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .includedServices(included)
                .build();

        service = serviceCatalogRepository.save(service);
        return mapToResponse(service);
    }

    @Transactional
    public ServiceCatalogResponse updateService(Long id, ServiceCatalogRequest request) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog not found with id: " + id));

        boolean isSystemPackage = List.of("PKG-STD", "PKG-DELUXE", "PKG-ULTIMATE").contains(service.getServiceCode());
        if (isSystemPackage) {
            if (!service.getServiceCode().equals(request.getServiceCode())) {
                throw new BadRequestException("Không thể thay đổi mã dịch vụ của gói hệ thống cốt lõi!");
            }
            if (request.getIsActive() != null && !request.getIsActive()) {
                throw new BadRequestException("Không thể tắt hoạt động của gói dịch vụ hệ thống cốt lõi!");
            }
        }

        if (!service.getServiceCode().equals(request.getServiceCode()) &&
                serviceCatalogRepository.existsByServiceCode(request.getServiceCode())) {
            throw new BadRequestException("Service code '" + request.getServiceCode() + "' already exists");
        }

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : (service.getDurationMinutes() != null ? service.getDurationMinutes() : 15);

        if (request.getServiceType() == ServiceType.PACKAGE) {
            if (request.getIncludedServiceIds() == null || request.getIncludedServiceIds().isEmpty()) {
                throw new BadRequestException("Gói Combo bắt buộc phải chọn ít nhất 1 dịch vụ con!");
            }
            List<ServiceCatalog> included = serviceCatalogRepository.findAllById(request.getIncludedServiceIds());
            boolean hasNestedPackage = included.stream().anyMatch(s -> s.getServiceType() == ServiceType.PACKAGE);
            if (hasNestedPackage) {
                throw new BadRequestException("Gói Combo chỉ được bao gồm các dịch vụ đơn lẻ, không được chứa gói Combo khác!");
            }

            int sumDuration = included.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
            if ((request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) && sumDuration > 0) {
                duration = sumDuration;
            }
            service.setIncludedServices(included);
        }

        service.setServiceCode(request.getServiceCode());
        service.setServiceName(request.getServiceName());
        service.setServiceType(request.getServiceType());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(duration);
        service.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }
        if (request.getDisplayOrder() != null) {
            service.setDisplayOrder(request.getDisplayOrder());
        }

        return mapToResponse(serviceCatalogRepository.save(service));
    }

    @Transactional
    public ServiceCatalogResponse toggleStatus(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog not found with id: " + id));

        if (List.of("PKG-STD", "PKG-DELUXE", "PKG-ULTIMATE").contains(service.getServiceCode())) {
            throw new BadRequestException("Không thể tắt hoạt động của gói dịch vụ hệ thống cốt lõi!");
        }
        service.setIsActive(!service.getIsActive());
        return mapToResponse(serviceCatalogRepository.save(service));
    }

    @Transactional
    public void deleteService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog not found with id: " + id));

        if (List.of("PKG-STD", "PKG-DELUXE", "PKG-ULTIMATE").contains(service.getServiceCode())) {
            throw new BadRequestException("Không thể xóa gói dịch vụ hệ thống cốt lõi!");
        }

        if (serviceCatalogRepository.countPackageReferences(id) > 0) {
            throw new BadRequestException("Không thể xóa vĩnh viễn dịch vụ này vì đang nằm trong ít nhất 1 Gói Combo! Hãy sử dụng tính năng 'Tắt hoạt động' thay vì xóa.");
        }

        if (bookingItemRepository.existsByService_ServiceId(id)) {
            throw new BadRequestException("Không thể xóa vĩnh viễn dịch vụ này vì đã từng có đơn hàng đặt lịch! Hãy sử dụng tính năng 'Tắt hoạt động' thay vì xóa.");
        }

        serviceCatalogRepository.delete(service);
    }

    public ServiceCatalogResponse mapToResponse(ServiceCatalog entity) {
        List<ServiceCatalogResponse> included = null;
        if (entity.getIncludedServices() != null && !entity.getIncludedServices().isEmpty()) {
            included = entity.getIncludedServices().stream()
                    .map(srv -> ServiceCatalogResponse.builder()
                            .serviceId(srv.getServiceId())
                            .serviceCode(srv.getServiceCode())
                            .serviceName(srv.getServiceName())
                            .serviceType(srv.getServiceType())
                            .price(srv.getPrice())
                            .durationMinutes(srv.getDurationMinutes())
                            .description(srv.getDescription())
                            .isActive(srv.getIsActive())
                            .displayOrder(srv.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return ServiceCatalogResponse.builder()
                .serviceId(entity.getServiceId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .serviceType(entity.getServiceType())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .includedServices(included)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
