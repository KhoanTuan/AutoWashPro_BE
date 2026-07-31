package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceCatalog;
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

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAllServices(boolean activeOnly) {
        List<ServiceCatalog> catalog = activeOnly
                ? serviceCatalogRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                : serviceCatalogRepository.findAllByOrderByDisplayOrderAsc();
        return catalog.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ServiceCatalogResponse createService(ServiceCatalogRequest request) {
        if (serviceCatalogRepository.existsByServiceCode(request.getServiceCode())) {
            throw new BadRequestException("Service code '" + request.getServiceCode() + "' already exists");
        }

        ServiceCatalog service = ServiceCatalog.builder()
                .serviceCode(request.getServiceCode())
                .serviceName(request.getServiceName())
                .serviceType(request.getServiceType())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
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

        service.setServiceCode(request.getServiceCode());
        service.setServiceName(request.getServiceName());
        service.setServiceType(request.getServiceType());
        service.setPrice(request.getPrice());
        service.setDurationMinutes(request.getDurationMinutes());
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
