package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.OrderItemResponse;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.OrderItem;
import com.techcraft.techcraftbackend.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {ProductMapper.class, PcCaseMapper.class})
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userPhone", source = "user.phone")
    @Mapping(target = "totalQuantity", expression = "java(calculateTotalQuantity(order))")
    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "itemType", expression = "java(resolveItemType(item))")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "productImage", expression = "java(extractPrimaryImageUrl(item))")
    @Mapping(target = "pcCaseId", source = "pcCase.id")
    @Mapping(target = "pcCaseName", source = "pcCase.name")
    @Mapping(target = "useCase", source = "pcCase.useCase")
    @Mapping(target = "pcCase", source = "pcCase")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    OrderItemResponse toItemResponse(OrderItem item);

    default String resolveItemType(OrderItem item) {
        if (item == null) {
            return null;
        }
        return item.getPcCase() != null ? "PC_CASE" : "PRODUCT";
    }

    default String extractPrimaryImageUrl(OrderItem item) {
        if (item == null || item.getProduct() == null || item.getProduct().getImages() == null || item.getProduct().getImages().isEmpty()) {
            return null;
        }
        return item.getProduct().getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> item.getProduct().getImages().get(0).getImageUrl());
    }

    default BigDecimal calculateSubtotal(OrderItem item) {
        if (item == null || item.getUnitPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    default Integer calculateTotalQuantity(Order order) {
        if (order == null || order.getItems() == null) {
            return 0;
        }
        return order.getItems().stream()
                .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                .sum();
    }
}
