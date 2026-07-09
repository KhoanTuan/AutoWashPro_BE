package com.autowashpro.autowashpro_be.modules.marketing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedbackCreateRequest {

    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String bookingCode;

    private String serviceName;

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    private Integer ratingStars;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    private String comment;
}
