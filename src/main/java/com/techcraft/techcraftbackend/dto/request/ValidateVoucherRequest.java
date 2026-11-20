package com.techcraft.techcraftbackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateVoucherRequest {

    @NotBlank(message = "Mã giảm giá không được để trống.")
    private String code;

    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng không được âm.")
    private BigDecimal orderAmount;
}
