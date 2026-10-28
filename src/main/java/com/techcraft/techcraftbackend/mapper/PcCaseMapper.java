package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.PcCaseItemResponse;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Review;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ProductMapper.class, ReviewMapper.class})
public interface PcCaseMapper {

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    PcCaseResponse toResponse(PcCase pcCase);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "product", source = "product")
    PcCaseItemResponse toItemResponse(PcCaseItem item);

    @AfterMapping
    default void calculateReviewStats(PcCase pcCase, @MappingTarget PcCaseResponse response) {
        if (pcCase.getReviews() == null || pcCase.getReviews().isEmpty()) {
            response.setAverageRating(0.0);
            response.setTotalReviews(0);
        } else {
            double avg = pcCase.getReviews().stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            response.setAverageRating(Math.round(avg * 10.0) / 10.0);
            response.setTotalReviews(pcCase.getReviews().size());
        }
    }
}
