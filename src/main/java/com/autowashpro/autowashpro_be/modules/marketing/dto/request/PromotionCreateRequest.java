package com.autowashpro.autowashpro_be.modules.marketing.dto.request;

import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCreateRequest {

    @NotBlank(message = "Mã Voucher không được để trống")
    private String code;

    @NotBlank(message = "Tên chiến dịch không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Kiểu chiết khấu không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal value;

    private Integer costPoints;
    private String minTier;
    private Integer minRecencyDays;
    private Integer maxClaimPerUser;
    private Integer totalBudget;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
