package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.DiscountType;
import jakarta.validation.constraints.*;
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
public class CreateVoucherRequest {

    @NotBlank(message = "Mã giảm giá không được để trống.")
    @Size(min = 2, max = 50, message = "Mã giảm giá phải từ 2 đến 50 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã giảm giá chỉ được chứa chữ cái, số, dấu gạch nối và gạch dưới.")
    private String code;

    @NotBlank(message = "Tên chương trình voucher không được để trống.")
    @Size(max = 255, message = "Tên chương trình voucher không được vượt quá 255 ký tự.")
    private String name;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống (PERCENTAGE hoặc FIXED_AMOUNT).")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống.")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0.")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Mức giảm tối đa không được âm.")
    private BigDecimal maxDiscountAmount;

    @Builder.Default
    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng tối thiểu không được âm.")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @NotNull(message = "Thời gian bắt đầu không được để trống.")
    private Instant startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống.")
    private Instant endDate;

    @Min(value = 1, message = "Tổng số lượt sử dụng tối thiểu là 1.")
    private Integer usageLimit;

    @Builder.Default
    @Min(value = 1, message = "Số lượt sử dụng cho mỗi khách hàng tối thiểu là 1.")
    private Integer userUsageLimit = 1;

    @Builder.Default
    private Boolean isActive = true;
}
