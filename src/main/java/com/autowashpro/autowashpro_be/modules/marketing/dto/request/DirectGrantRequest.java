package com.autowashpro.autowashpro_be.modules.marketing.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectGrantRequest {

    @NotEmpty(message = "Danh sách khách hàng nhận không được để trống")
    private List<Long> customerIds;

    @NotNull(message = "ID chiến dịch khuyến mãi không được để trống")
    private Long promotionId;

    private String reason;
}
