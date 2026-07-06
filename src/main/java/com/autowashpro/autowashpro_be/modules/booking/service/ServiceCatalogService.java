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
        service.setIsActive(!service.getIsActive());
        return mapToResponse(serviceCatalogRepository.save(service));
    }

    public ServiceCatalogResponse mapToResponse(ServiceCatalog entity) {
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
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
