package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.entity.*;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAdminService {

    private static final String DEFAULT_PASSWORD = "Customer@123";

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final CustomerMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> listCustomers(String status, String keyword, int page, int size) {
        CustomerStatus customerStatus = parseStatus(status);
        Page<Customer> result = customerRepository.search(
                customerStatus,
                keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<CustomerResponse> content = result.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        return PageResponse.<CustomerResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerOptionResponse> listOptions() {
        return customerRepository.findAll(Sort.by("fullName").ascending()).stream()
                .filter(c -> c.getStatus() == CustomerStatus.ACTIVE)
                .map(mapper::toOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Customer customer = findCustomer(id);
        return mapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryStatsResponse getSummaryStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime churnThreshold = LocalDateTime.now().minusDays(60);

        long total = customerRepository.count();
        long active = customerRepository.countByStatus(CustomerStatus.ACTIVE);
        long vip = customerRepository.findAll().stream()
                .filter(c -> c.getTier() != null && !"REGULAR".equals(c.getTier().getTierName()))
                .count();

        return CustomerSummaryStatsResponse.builder()
                .totalCustomers(total)
                .activeCustomers(active)
                .vipMembers(vip)
                .newRegistrationsLast30Days(customerRepository.countRegisteredSince(thirtyDaysAgo))
                .churnRiskCount(customerRepository.countChurnRisk(CustomerStatus.ACTIVE, churnThreshold))
                .build();
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String phone = normalizePhone(request.getPhoneNumber());
        if (customerRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String plate = normalizePlate(request.getLicensePlate());
        if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
            throw new BadRequestException("License plate already exists");
        }

        LoyaltyTier tier = loyaltyTierRepository.findByTierName("REGULAR")
                .orElseThrow(() -> new ResourceNotFoundException("Default loyalty tier not found"));

        CustomerStatus status = request.getStatus() != null && !request.getStatus().isBlank()
                ? CustomerStatus.valueOf(request.getStatus().toUpperCase())
                : CustomerStatus.ACTIVE;

        Customer customer = Customer.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(phone)
                .email(blankToNull(request.getEmail()))
                .authProvider(CustomerAuthProvider.PHONE)
                .status(status)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .tier(tier)
                .build();
        customerRepository.save(customer);

        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .licensePlate(plate)
                .carType(mapper.parseCarType(request.getCarType()))
                .build();
        vehicleRepository.save(vehicle);

        return mapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);
        String phone = normalizePhone(request.getPhoneNumber());

        if (!customer.getPhoneNumber().equals(phone) && customerRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && (customer.getEmail() == null || !customer.getEmail().equalsIgnoreCase(request.getEmail()))
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        customer.setFullName(request.getFullName().trim());
        customer.setPhoneNumber(phone);
        customer.setEmail(blankToNull(request.getEmail()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            customer.setStatus(CustomerStatus.valueOf(request.getStatus().toUpperCase()));
        }

        String plate = normalizePlate(request.getLicensePlate());
        Vehicle primary = vehicleRepository.findFirstByCustomerCustomerIdOrderByCreatedAtAsc(id).orElse(null);
        if (primary != null) {
            if (!primary.getLicensePlate().equalsIgnoreCase(plate)
                    && vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
                throw new BadRequestException("License plate already exists");
            }
            primary.setLicensePlate(plate);
            primary.setCarType(mapper.parseCarType(request.getCarType()));
            vehicleRepository.save(primary);
        } else {
            if (vehicleRepository.existsByLicensePlateIgnoreCase(plate)) {
                throw new BadRequestException("License plate already exists");
            }
            vehicleRepository.save(Vehicle.builder()
                    .customer(customer)
                    .licensePlate(plate)
                    .carType(mapper.parseCarType(request.getCarType()))
                    .build());
        }

        customerRepository.save(customer);
        return mapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, String status) {
        Customer customer = findCustomer(id);
        customer.setStatus(CustomerStatus.valueOf(status.toUpperCase()));
        customerRepository.save(customer);
        return mapper.toResponse(customer);
    }

    @Transactional
    public Customer resolveOrCreateForBooking(Long customerId, String fullName, String phone, String email,
                                              String licensePlate, CarType carType) {
        if (customerId != null) {
            Customer existing = findCustomer(customerId);
            upsertVehicle(existing, licensePlate, carType);
            return existing;
        }

        String normalizedPhone = phone != null ? normalizePhone(phone) : null;
        if (normalizedPhone != null) {
            Customer byPhone = customerRepository.findByPhoneNumber(normalizedPhone).orElse(null);
            if (byPhone != null) {
                if (fullName != null && !fullName.isBlank()) {
                    byPhone.setFullName(fullName.trim());
                }
                if (email != null && !email.isBlank()) {
                    byPhone.setEmail(blankToNull(email));
                }
                upsertVehicle(byPhone, licensePlate, carType);
                customerRepository.save(byPhone);
                return byPhone;
            }
        }

        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setFullName(fullName != null && !fullName.isBlank() ? fullName : "Walk-in Guest");
        req.setPhoneNumber(normalizedPhone != null ? normalizedPhone : generateWalkInPhone());
        req.setEmail(email);
        req.setLicensePlate(licensePlate);
        req.setCarType(carType.name());
        return customerRepository.findById(createCustomer(req).getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found after create"));
    }

    @Transactional
    public Vehicle resolveVehicle(Customer customer, String licensePlate, CarType carType) {
        String plate = normalizePlate(licensePlate);
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate).orElse(null);
        if (vehicle != null) {
            if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new BadRequestException("License plate belongs to another customer");
            }
            vehicle.setCarType(carType);
            return vehicleRepository.save(vehicle);
        }
        return vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .licensePlate(plate)
                .carType(carType)
                .build());
    }

    private void upsertVehicle(Customer customer, String licensePlate, CarType carType) {
        if (licensePlate == null || licensePlate.isBlank()) {
            return;
        }
        resolveVehicle(customer, licensePlate, carType);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private CustomerStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return CustomerStatus.valueOf(status.toUpperCase());
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("\\s+", "");
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateWalkInPhone() {
        return "W" + System.currentTimeMillis() % 1_000_000_000_000L;
    }
}
