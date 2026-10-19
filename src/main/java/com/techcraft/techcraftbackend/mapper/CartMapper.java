package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.Objects;

@Mapper(componentModel = "spring", uses = {ProductMapper.class, PcCaseMapper.class})
public interface CartMapper {

    @Mapping(target = "itemType", expression = "java(resolveItemType(item))")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "productImage", expression = "java(extractPrimaryImageUrl(item))")
    @Mapping(target = "pcCaseId", source = "pcCase.id")
    @Mapping(target = "pcCaseName", source = "pcCase.name")
    @Mapping(target = "useCase", source = "pcCase.useCase")
    @Mapping(target = "pcCase", source = "pcCase")
    @Mapping(target = "unitPrice", expression = "java(resolveUnitPrice(item))")
    @Mapping(target = "stockQuantity", expression = "java(resolveStockQuantity(item))")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    CartItemResponse toResponse(CartItem item);

    default String resolveItemType(CartItem item) {
        if (item == null) {
            return null;
        }
        return item.getPcCase() != null ? "PC_CASE" : "PRODUCT";
    }

    default BigDecimal resolveUnitPrice(CartItem item) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        if (item.getProduct() != null && item.getProduct().getPrice() != null) {
            return item.getProduct().getPrice();
        }
        if (item.getPcCase() != null && item.getPcCase().getTotalPrice() != null) {
            return item.getPcCase().getTotalPrice();
        }
        return BigDecimal.ZERO;
    }

    default Integer resolveStockQuantity(CartItem item) {
        if (item == null) {
            return 0;
        }
        if (item.getProduct() != null) {
            return item.getProduct().getStockQuantity() != null ? item.getProduct().getStockQuantity() : 0;
        }
        if (item.getPcCase() != null && item.getPcCase().getItems() != null && !item.getPcCase().getItems().isEmpty()) {
            return item.getPcCase().getItems().stream()
                    .map(PcCaseItem::getProduct)
                    .filter(Objects::nonNull)
                    .mapToInt(p -> (p.isActive() && p.getStockQuantity() != null) ? p.getStockQuantity() : 0)
                    .min()
                    .orElse(0);
        }
        return 0;
    }

    default String extractPrimaryImageUrl(CartItem item) {
        if (item == null || item.getProduct() == null || item.getProduct().getImages() == null || item.getProduct().getImages().isEmpty()) {
            return null;
        }
        return item.getProduct().getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> item.getProduct().getImages().get(0).getImageUrl());
    }

    default BigDecimal calculateSubtotal(CartItem item) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal unitPrice = resolveUnitPrice(item);
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
