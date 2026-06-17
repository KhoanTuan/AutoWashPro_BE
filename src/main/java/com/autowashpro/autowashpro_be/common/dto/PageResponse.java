package com.autowashpro.autowashpro_be.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Phản hồi phân trang — dùng cho danh sách nhân viên")
public class PageResponse<T> {

    @Schema(description = "Danh sách bản ghi trang hiện tại")
    private List<T> content;

    @Schema(description = "Trang hiện tại (0-based)", example = "0")
    private int page;

    @Schema(description = "Kích thước trang", example = "10")
    private int size;

    @Schema(description = "Tổng số bản ghi", example = "25")
    private long totalElements;

    @Schema(description = "Tổng số trang", example = "3")
    private int totalPages;
}
