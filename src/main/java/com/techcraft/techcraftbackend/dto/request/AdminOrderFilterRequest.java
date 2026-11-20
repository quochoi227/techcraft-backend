package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
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
public class AdminOrderFilterRequest {

    private OrderStatus status;

    private String search;

    private String keyword;

    private PaymentMethod paymentMethod;

    @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Số lượng đơn hàng trên trang (size) tối thiểu là 1")
    @Max(value = 100, message = "Số lượng đơn hàng trên trang (size) tối đa là 100")
    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDir = "desc";

    public String getEffectiveSearch() {
        if (search != null && !search.isBlank()) {
            return search.trim();
        }
        if (keyword != null && !keyword.isBlank()) {
            return keyword.trim();
        }
        return null;
    }

    public void setSort_by(String sort_by) {
        if (sort_by != null && !sort_by.isBlank()) {
            this.sortBy = sort_by;
        }
    }

    public void setSort_dir(String sort_dir) {
        if (sort_dir != null && !sort_dir.isBlank()) {
            this.sortDir = sort_dir;
        }
    }

    public void setPayment_method(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
