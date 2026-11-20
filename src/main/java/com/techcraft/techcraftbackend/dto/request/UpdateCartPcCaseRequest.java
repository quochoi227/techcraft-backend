package com.techcraft.techcraftbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartPcCaseRequest {

    @NotNull(message = "Số lượng cấu hình PC không được để trống")
    @Min(value = 1, message = "Số lượng cấu hình PC phải lớn hơn hoặc bằng 1")
    @JsonAlias({"quantity"})
    private Integer quantity;
}
