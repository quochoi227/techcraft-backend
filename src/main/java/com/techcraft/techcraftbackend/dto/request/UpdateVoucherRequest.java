package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherRequest {

    @Size(max = 255, message = "Tên chương trình voucher không được vượt quá 255 ký tự.")
    private String name;

    private String description;

    private DiscountType discountType;

    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0.")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Mức giảm tối đa không được âm.")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng tối thiểu không được âm.")
    private BigDecimal minOrderAmount;

    private Instant startDate;

    private Instant endDate;

    @Min(value = 1, message = "Tổng số lượt sử dụng tối thiểu là 1.")
    private Integer usageLimit;

    @Min(value = 1, message = "Số lượt sử dụng cho mỗi khách hàng tối thiểu là 1.")
    private Integer userUsageLimit;

    private Boolean isActive;
}
