package com.techcraft.techcraftbackend.config;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StringToProductCategoryConverter implements Converter<String, ProductCategory> {

    @Override
    public ProductCategory convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        String normalized = source.trim().replace("-", "_").toUpperCase();
        try {
            return ProductCategory.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Danh mục '" + source + "' không hợp lệ. Các danh mục hợp lệ: " + Arrays.toString(ProductCategory.values()));
        }
    }
}
