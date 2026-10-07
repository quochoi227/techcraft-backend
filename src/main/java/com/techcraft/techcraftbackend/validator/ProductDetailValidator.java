package com.techcraft.techcraftbackend.validator;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.techcraft.techcraftbackend.dto.detail.*;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDetailValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public Map<String, Object> validateAndNormalize(ProductCategory category, Map<String, Object> rawDetail) {
        if (category == null) {
            throw new BadRequestException("Danh mục sản phẩm không được để trống");
        }
        if (rawDetail == null || rawDetail.isEmpty()) {
            throw new BadRequestException("Thông số kỹ thuật chi tiết (detail) không được để trống");
        }

        Object detailDto = parseDetailDto(category, rawDetail);
        validateDto(detailDto);
        validateSpecialCategoryRules(category, detailDto);

        return objectMapper.convertValue(detailDto, new TypeReference<Map<String, Object>>() {});
    }

    private Object parseDetailDto(ProductCategory category, Map<String, Object> rawDetail) {
        try {
            return switch (category) {
                case CPU -> objectMapper.convertValue(rawDetail, CpuDetail.class);
                case MAINBOARD -> objectMapper.convertValue(rawDetail, MainboardDetail.class);
                case RAM -> objectMapper.convertValue(rawDetail, RamDetail.class);
                case GPU -> objectMapper.convertValue(rawDetail, GpuDetail.class);
                case STORAGE -> objectMapper.convertValue(rawDetail, StorageDetail.class);
                case PSU -> objectMapper.convertValue(rawDetail, PsuDetail.class);
                case CASE -> objectMapper.convertValue(rawDetail, CaseDetail.class);
                case CPU_COOLER -> objectMapper.convertValue(rawDetail, CpuCoolerDetail.class);
            };
        } catch (IllegalArgumentException ex) {
            log.error("Failed to parse product detail for category {}: {}", category, ex.getMessage());
            throw new BadRequestException("Định dạng thông số kỹ thuật không hợp lệ cho danh mục " + category + ": " + ex.getMessage());
        }
    }

    private void validateDto(Object dto) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new BadRequestException("Thông số kỹ thuật không hợp lệ: " + errorMessage);
        }
    }

    private void validateSpecialCategoryRules(ProductCategory category, Object dto) {
        if (category == ProductCategory.CPU && dto instanceof CpuDetail cpu) {
            if (cpu.getBoostClockGhz() != null && cpu.getBaseClockGhz() != null
                    && cpu.getBoostClockGhz() < cpu.getBaseClockGhz()) {
                throw new BadRequestException("Xung nhịp tối đa (boost_clock_ghz) phải lớn hơn hoặc bằng xung nhịp cơ bản (base_clock_ghz)");
            }
        } else if (category == ProductCategory.CPU_COOLER && dto instanceof CpuCoolerDetail cooler) {
            String type = cooler.getType() != null ? cooler.getType().trim().toLowerCase() : "";
            if (type.contains("air") && cooler.getHeightMm() == null) {
                throw new BadRequestException("Tản nhiệt khí (Air) phải có thông số chiều cao (height_mm)");
            }
            if (type.contains("aio") || type.contains("liquid")) {
                if (cooler.getRadiatorSizeMm() == null) {
                    throw new BadRequestException("Tản nhiệt nước (AIO Liquid) phải có kích thước radiator (radiator_size_mm)");
                }
            }
        }
    }
}
