package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "stockQuantity", source = "product.stockQuantity")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "productImage", expression = "java(extractPrimaryImageUrl(item))")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    CartItemResponse toResponse(CartItem item);

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
        if (item == null || item.getProduct() == null || item.getProduct().getPrice() == null) {
            return BigDecimal.ZERO;
        }
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        return item.getProduct().getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
