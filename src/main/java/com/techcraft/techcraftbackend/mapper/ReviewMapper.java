package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.ReviewResponse;
import com.techcraft.techcraftbackend.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "pcCaseId", source = "pcCase.id")
    @Mapping(target = "pcCaseName", source = "pcCase.name")
    ReviewResponse toResponse(Review review);
}
