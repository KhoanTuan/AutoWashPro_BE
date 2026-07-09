package com.autowashpro.autowashpro_be.modules.marketing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResolveRequest {

    @NotBlank(message = "Ghi chú giải trình không được để trống")
    private String resolutionNotes;

    private boolean grantCompensationVoucher;
    private String voucherCode;
    private BigDecimal discountValue;
}
