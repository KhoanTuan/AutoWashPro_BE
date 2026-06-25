package com.autowashpro.autowashpro_be.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Phản hồi thông báo đơn giản")
public class MessageResponse {

    @Schema(description = "Nội dung thông báo", example = "Password reset successfully")
    private String message;

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
