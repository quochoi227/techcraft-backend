package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.DiscountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VoucherFilterRequest {

    private String search;

    private String keyword;

    private DiscountType discountType;

    @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Số lượng trên trang (size) tối thiểu là 1")
    @Max(value = 100, message = "Số lượng trên trang (size) tối đa là 100")
    @Builder.Default
    private int size = 20;

    public String getEffectiveSearch() {
        if (search != null && !search.isBlank()) {
            return search.trim();
        }
        if (keyword != null && !keyword.isBlank()) {
            return keyword.trim();
        }
        return null;
    }

    public void setDiscount_type(DiscountType discountType) {
        this.discountType = discountType;
    }
}
