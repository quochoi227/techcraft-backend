package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.PcCaseItemResponse;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface PcCaseMapper {

    @Mapping(target = "isActive", source = "active")
    PcCaseResponse toResponse(PcCase pcCase);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "product", source = "product")
    PcCaseItemResponse toItemResponse(PcCaseItem item);
}
