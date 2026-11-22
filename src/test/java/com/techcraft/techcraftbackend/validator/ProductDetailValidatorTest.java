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
        cpuDetail.put("baseClockGhz", 3.6);
        cpuDetail.put("boostClockGhz", 5.2);
        cpuDetail.put("tdpW", 65);
        cpuDetail.put("memoryType", "DDR4/DDR5");
        cpuDetail.put("integratedGpu", true);

        Map<String, Object> result = validator.validateAndNormalize(ProductCategory.CPU, cpuDetail);

        assertNotNull(result);
        assertEquals("LGA1700", result.get("socket"));
        assertEquals(8, result.get("cores"));
        assertEquals(true, result.get("integratedGpu"));
    }

    @Test
    void validateAndNormalize_CpuMissingSocket_ThrowsBadRequestException() {
        Map<String, Object> cpuDetail = new HashMap<>();
        cpuDetail.put("cores", 8);
        cpuDetail.put("threads", 16);
        cpuDetail.put("baseClockGhz", 3.6);
        cpuDetail.put("boostClockGhz", 5.2);
        cpuDetail.put("tdpW", 65);
        cpuDetail.put("memoryType", "DDR4/DDR5");
        cpuDetail.put("integratedGpu", true);

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
        cpuDetail.put("baseClockGhz", 4.0);
        cpuDetail.put("boostClockGhz", 3.5);
        cpuDetail.put("tdpW", 65);
        cpuDetail.put("memoryType", "DDR5");
        cpuDetail.put("integratedGpu", false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validateAndNormalize(ProductCategory.CPU, cpuDetail)
        );
        assertTrue(ex.getMessage().contains("phải lớn hơn hoặc bằng"));
    }

    @Test
    void validateAndNormalize_ValidGpu_Success() {
        Map<String, Object> gpuDetail = new HashMap<>();
        gpuDetail.put("vramGb", 12);
        gpuDetail.put("vramType", "GDDR6X");
        gpuDetail.put("tdpW", 200);
        gpuDetail.put("pcieSlot", "x16");
        gpuDetail.put("lengthMm", 320);

        Map<String, Object> result = validator.validateAndNormalize(ProductCategory.GPU, gpuDetail);

        assertNotNull(result);
        assertEquals(12, result.get("vramGb"));
        assertEquals("GDDR6X", result.get("vramType"));
    }

    @Test
    void validateAndNormalize_AirCoolerWithoutHeight_ThrowsBadRequestException() {
        Map<String, Object> coolerDetail = new HashMap<>();
        coolerDetail.put("type", "Air");
        coolerDetail.put("socketSupport", List.of("LGA1700", "AM5"));
        coolerDetail.put("tdpSupportW", 200);

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
