package com.techcraft.techcraftbackend.validator;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductDetailValidatorTest {

    private ProductDetailValidator validator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        Validator jakartaValidator = Validation.buildDefaultValidatorFactory().getValidator();
        validator = new ProductDetailValidator(objectMapper, jakartaValidator);
    }

    @Test
    void validateAndNormalize_ValidCpu_ReturnsNormalizedMap() {
        Map<String, Object> cpuDetail = new HashMap<>();
        cpuDetail.put("socket", "LGA1700");
        cpuDetail.put("cores", 8);
        cpuDetail.put("threads", 16);
        cpuDetail.put("base_clock_ghz", 3.6);
        cpuDetail.put("boost_clock_ghz", 5.2);
        cpuDetail.put("tdp_w", 65);
        cpuDetail.put("memory_type", "DDR4/DDR5");
        cpuDetail.put("integrated_gpu", true);

        Map<String, Object> result = validator.validateAndNormalize(ProductCategory.CPU, cpuDetail);

        assertNotNull(result);
        assertEquals("LGA1700", result.get("socket"));
        assertEquals(8, result.get("cores"));
        assertEquals(true, result.get("integrated_gpu"));
    }

    @Test
    void validateAndNormalize_CpuMissingSocket_ThrowsBadRequestException() {
        Map<String, Object> cpuDetail = new HashMap<>();
        cpuDetail.put("cores", 8);
        cpuDetail.put("threads", 16);
        cpuDetail.put("base_clock_ghz", 3.6);
        cpuDetail.put("boost_clock_ghz", 5.2);
        cpuDetail.put("tdp_w", 65);
        cpuDetail.put("memory_type", "DDR4/DDR5");
        cpuDetail.put("integrated_gpu", true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validateAndNormalize(ProductCategory.CPU, cpuDetail)
        );
        assertTrue(ex.getMessage().contains("Socket"));
    }

    @Test
    void validateAndNormalize_CpuBoostClockLessThanBaseClock_ThrowsBadRequestException() {
        Map<String, Object> cpuDetail = new HashMap<>();
        cpuDetail.put("socket", "LGA1700");
        cpuDetail.put("cores", 8);
        cpuDetail.put("threads", 16);
        cpuDetail.put("base_clock_ghz", 4.0);
        cpuDetail.put("boost_clock_ghz", 3.5);
        cpuDetail.put("tdp_w", 65);
        cpuDetail.put("memory_type", "DDR5");
        cpuDetail.put("integrated_gpu", false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validateAndNormalize(ProductCategory.CPU, cpuDetail)
        );
        assertTrue(ex.getMessage().contains("Xung nhịp tối đa"));
    }

    @Test
    void validateAndNormalize_ValidGpu_Success() {
        Map<String, Object> gpuDetail = new HashMap<>();
        gpuDetail.put("vram_gb", 12);
        gpuDetail.put("vram_type", "GDDR6X");
        gpuDetail.put("tdp_w", 200);
        gpuDetail.put("pcie_slot", "x16");
        gpuDetail.put("length_mm", 320);

        Map<String, Object> result = validator.validateAndNormalize(ProductCategory.GPU, gpuDetail);

        assertNotNull(result);
        assertEquals(12, result.get("vram_gb"));
        assertEquals("GDDR6X", result.get("vram_type"));
    }

    @Test
    void validateAndNormalize_AirCoolerWithoutHeight_ThrowsBadRequestException() {
        Map<String, Object> coolerDetail = new HashMap<>();
        coolerDetail.put("type", "Air");
        coolerDetail.put("socket_support", List.of("LGA1700", "AM5"));
        coolerDetail.put("tdp_support_w", 200);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validateAndNormalize(ProductCategory.CPU_COOLER, coolerDetail)
        );
        assertTrue(ex.getMessage().contains("chiều cao"));
    }

    @Test
    void validateAndNormalize_NullDetail_ThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validateAndNormalize(ProductCategory.CPU, null)
        );
        assertTrue(ex.getMessage().contains("không được để trống"));
    }
}
