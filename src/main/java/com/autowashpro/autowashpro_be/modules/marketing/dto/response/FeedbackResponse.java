package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import com.autowashpro.autowashpro_be.modules.marketing.entity.FeedbackStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerAvatar;
    private String bookingId;
    private String serviceName;
    private Integer ratingStars;
    private String comment;
    private LocalDateTime createdAt;
    private FeedbackStatus status;
    private String resolutionNotes;
    private String compensationVoucherCode;
}
