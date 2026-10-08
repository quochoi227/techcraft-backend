package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductFilterRequest {

    private String search;
    private String keyword;
    private ProductCategory category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
    private Boolean isActive;

    @Min(value = 0, message = "Số trang (page) phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Số lượng sản phẩm trên trang (size) tối thiểu là 1")
    @Max(value = 100, message = "Số lượng sản phẩm trên trang (size) tối đa là 100")
    @Builder.Default
    private int size = 12;

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

    public void setMin_price(BigDecimal min_price) {
        if (this.minPrice == null) {
            this.minPrice = min_price;
        }
    }

    public void setMax_price(BigDecimal max_price) {
        if (this.maxPrice == null) {
            this.maxPrice = max_price;
        }
    }

    public void setIn_stock(Boolean in_stock) {
        if (this.inStock == null) {
            this.inStock = in_stock;
        }
    }

    public void setIs_active(Boolean is_active) {
        if (this.isActive == null) {
            this.isActive = is_active;
        }
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
}
