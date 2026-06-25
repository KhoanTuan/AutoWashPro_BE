package com.autowashpro.autowashpro_be.modules.financial.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ShiftClosureResponse {

    private Long shiftClosureId;
    private Long cashierId;
    private String cashierName;
    private LocalDate shiftDate;
    private BigDecimal openingBalance;
    private BigDecimal expectedBalance;
    private BigDecimal actualBalance;
    private BigDecimal variance;
    private BigDecimal totalCash;
    private BigDecimal totalMomo;
    private BigDecimal totalRevenue;
    private String status;
    private LocalDateTime closedAt;
    private String notes;
}
