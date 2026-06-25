package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.common.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.financial.dto.OpenShiftRequest;
import com.autowashpro.autowashpro_be.modules.financial.dto.ShiftClosureRequest;
import com.autowashpro.autowashpro_be.modules.financial.dto.ShiftClosureResponse;
import com.autowashpro.autowashpro_be.modules.financial.entity.ShiftClosure;
import com.autowashpro.autowashpro_be.modules.financial.entity.ShiftClosureStatus;
import com.autowashpro.autowashpro_be.modules.financial.repository.ShiftClosureRepository;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShiftClosureService {

    private static final BigDecimal FLAG_THRESHOLD = new BigDecimal("1000");

    private final ShiftClosureRepository shiftClosureRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public ShiftClosureResponse getCurrentShift() {
        Staff cashier = requireCurrentStaff();
        ShiftClosure shift = findOpenShift(cashier.getStaffId());
        return toResponse(shift);
    }

    @Transactional
    public ShiftClosureResponse openShift(OpenShiftRequest request) {
        Staff cashier = requireCurrentStaff();
        LocalDate today = LocalDate.now();

        shiftClosureRepository.findByCashierStaffIdAndShiftDateAndStatus(
                cashier.getStaffId(), today, ShiftClosureStatus.OPEN
        ).ifPresent(existing -> {
            throw new BadRequestException("Shift is already open for today");
        });

        BigDecimal openingBalance = request.getOpeningBalance() != null
                ? request.getOpeningBalance()
                : BigDecimal.ZERO;

        ShiftClosure shift = ShiftClosure.builder()
                .cashier(cashier)
                .shiftDate(today)
                .openingBalance(openingBalance)
                .status(ShiftClosureStatus.OPEN)
                .build();

        shiftClosureRepository.save(shift);
        return toResponse(shift);
    }

    @Transactional
    public ShiftClosureResponse closeShift(ShiftClosureRequest request) {
        Staff cashier = requireCurrentStaff();
        ShiftClosure shift = findOpenShift(cashier.getStaffId());

        BigDecimal expectedBalance = shift.getOpeningBalance().add(shift.getTotalCash());
        BigDecimal actualBalance = request.getActualBalance();
        BigDecimal variance = actualBalance.subtract(expectedBalance);

        shift.setExpectedBalance(expectedBalance);
        shift.setActualBalance(actualBalance);
        shift.setVariance(variance);
        shift.setClosedAt(LocalDateTime.now());
        shift.setNotes(request.getNotes());

        if (variance.abs().compareTo(FLAG_THRESHOLD) > 0) {
            shift.setStatus(ShiftClosureStatus.FLAGGED);
        } else {
            shift.setStatus(ShiftClosureStatus.CLOSED);
        }

        shiftClosureRepository.save(shift);
        return toResponse(shift);
    }

    @Transactional
    public void recordCashCollection(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ShiftClosure shift = resolveOpenShiftForCurrentCashier();
        if (shift == null) {
            return;
        }
        shift.setTotalCash(shift.getTotalCash().add(amount));
        shift.setTotalRevenue(shift.getTotalRevenue().add(amount));
        shiftClosureRepository.save(shift);
    }

    @Transactional
    public void recordMomoCollection(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ShiftClosure shift = resolveOpenShiftForCurrentCashier();
        if (shift == null) {
            return;
        }
        shift.setTotalMomo(shift.getTotalMomo().add(amount));
        shift.setTotalRevenue(shift.getTotalRevenue().add(amount));
        shiftClosureRepository.save(shift);
    }

    private ShiftClosure resolveOpenShiftForCurrentCashier() {
        Staff cashier = getCurrentStaff();
        if (cashier == null) {
            return null;
        }
        return shiftClosureRepository
                .findByCashierStaffIdAndShiftDateAndStatus(cashier.getStaffId(), LocalDate.now(), ShiftClosureStatus.OPEN)
                .orElse(null);
    }

    private ShiftClosure findOpenShift(Long cashierId) {
        return shiftClosureRepository
                .findByCashierStaffIdAndShiftDateAndStatus(cashierId, LocalDate.now(), ShiftClosureStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("No open shift found for today"));
    }

    private Staff requireCurrentStaff() {
        Staff staff = getCurrentStaff();
        if (staff == null) {
            throw new BadRequestException("Cashier authentication required");
        }
        return staff;
    }

    private Staff getCurrentStaff() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        if (principal.getUserType() != UserPrincipal.UserType.STAFF) {
            return null;
        }
        return staffRepository.findById(principal.getId()).orElse(null);
    }

    private ShiftClosureResponse toResponse(ShiftClosure shift) {
        shift.getCashier().getFullName();
        return ShiftClosureResponse.builder()
                .shiftClosureId(shift.getShiftClosureId())
                .cashierId(shift.getCashier().getStaffId())
                .cashierName(shift.getCashier().getFullName())
                .shiftDate(shift.getShiftDate())
                .openingBalance(shift.getOpeningBalance())
                .expectedBalance(shift.getExpectedBalance())
                .actualBalance(shift.getActualBalance())
                .variance(shift.getVariance())
                .totalCash(shift.getTotalCash())
                .totalMomo(shift.getTotalMomo())
                .totalRevenue(shift.getTotalRevenue())
                .status(shift.getStatus().name())
                .closedAt(shift.getClosedAt())
                .notes(shift.getNotes())
                .build();
    }
}
