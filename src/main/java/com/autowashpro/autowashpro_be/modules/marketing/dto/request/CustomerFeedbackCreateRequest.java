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

    private String bookingCode;
    private Long bookingId;

    private String serviceName;

    private Integer ratingStars;
    private Integer rating;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    private String comment;
}
