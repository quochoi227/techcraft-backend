package com.techcraft.techcraftbackend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {

    private UUID productId;

    private UUID pcCaseId;

    @Min(value = 1, message = "Số lượng sản phẩm hoặc cấu hình PC phải lớn hơn hoặc bằng 1.")
    @Builder.Default
    private Integer quantity = 1;
}
