package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.response.ProductImageResponse;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.techcraft.techcraftbackend.dto.request.UpdateProductRequest;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", source = "isActive")
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product product);

    @Mapping(target = "isActive", source = "active")
    ProductResponse toResponse(Product product);

    @Mapping(target = "isPrimary", source = "primary")
    ProductImageResponse toImageResponse(ProductImage image);
}
