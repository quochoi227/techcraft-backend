package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompatibilityCheckerServiceTest {

    @Mock
    private ProductRepository productRepository;

    private CompatibilityCheckerService compatibilityCheckerService;

    private Map<ProductCategory, Product> validComponents;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        compatibilityCheckerService = new CompatibilityCheckerService(productRepository, objectMapper);

        validComponents = new EnumMap<>(ProductCategory.class);
        validComponents.put(ProductCategory.CPU, buildProduct(ProductCategory.CPU, "Intel Core i5-13400F", 10, Map.of(
                "socket", "LGA1700",
                "cores", 10,
                "threads", 16,
                "base_clock_ghz", 2.5,
                "boost_clock_ghz", 4.6,
                "tdp_w", 65,
                "memory_type", "DDR4/DDR5",
                "integrated_gpu", false
        )));
        validComponents.put(ProductCategory.MAINBOARD, buildProduct(ProductCategory.MAINBOARD, "MSI MAG B760M MORTAR WIFI II", 10, Map.of(
                "socket", "LGA1700",
                "form_factor", "mATX",
                "chipset", "B760",
                "memory_type", "DDR5",
                "memory_slots", 4,
                "max_memory_gb", 256,
                "m2_slots", 3,
                "pcie_version", "5.0"
        )));
        validComponents.put(ProductCategory.RAM, buildProduct(ProductCategory.RAM, "Corsair Vengeance RGB 32GB DDR5", 10, Map.of(
                "type", "DDR5",
                "speed_mhz", 6000,
                "capacity_gb", 16,
                "sticks", 2,
                "total_gb", 32
        )));
        validComponents.put(ProductCategory.GPU, buildProduct(ProductCategory.GPU, "MSI RTX 4060 VENTUS 2X", 10, Map.of(
                "vram_gb", 8,
                "vram_type", "GDDR6",
                "tdp_w", 115,
                "pcie_slot", "x16",
                "length_mm", 199
        )));
        validComponents.put(ProductCategory.STORAGE, buildProduct(ProductCategory.STORAGE, "Samsung 990 PRO 1TB", 10, Map.of(
                "type", "NVMe SSD",
                "capacity_gb", 1000,
                "interface", "M.2 PCIe 4.0",
                "read_mbps", 7450,
                "write_mbps", 6900
        )));
        validComponents.put(ProductCategory.PSU, buildProduct(ProductCategory.PSU, "MSI MAG A650BN 650W", 10, Map.of(
                "wattage_w", 650,
                "efficiency", "80+ Bronze",
                "modular", "Non-modular",
                "form_factor", "ATX"
        )));
        validComponents.put(ProductCategory.CASE, buildProduct(ProductCategory.CASE, "ASUS Prime AP201", 10, Map.of(
                "form_factor_support", List.of("mATX", "ITX"),
                "max_gpu_length_mm", 338,
                "max_cooler_height_mm", 170,
                "drive_bays_35", 3,
                "drive_bays_25", 4
        )));
        validComponents.put(ProductCategory.CPU_COOLER, buildProduct(ProductCategory.CPU_COOLER, "DeepCool AK400 Digital", 10, Map.of(
                "type", "Air",
                "socket_support", List.of("LGA1700", "AM5", "AM4"),
                "height_mm", 156,
                "tdp_support_w", 220
        )));
    }

    @Test
    void checkProducts_AllConstraintsSatisfied_ReturnsCompatibleTrue() {
        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertTrue(result.isCompatible());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void checkProducts_SocketMismatch_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU).setDetail(Map.of(
                "socket", "AM5",
                "cores", 6,
                "threads", 12,
                "base_clock_ghz", 4.7,
                "boost_clock_ghz", 5.3,
                "tdp_w", 105,
                "memory_type", "DDR5",
                "integrated_gpu", true
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Socket của CPU")));
    }

    @Test
    void checkProducts_CpuAndMainboardMemoryTypeMismatch_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU).setDetail(Map.of(
                "socket", "LGA1700",
                "cores", 10,
                "threads", 16,
                "base_clock_ghz", 2.5,
                "boost_clock_ghz", 4.6,
                "tdp_w", 65,
                "memory_type", "DDR4",
                "integrated_gpu", false
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Loại RAM hỗ trợ của CPU")));
    }

    @Test
    void checkProducts_RamAndMainboardMemoryTypeMismatch_ReturnsViolation() {
        validComponents.get(ProductCategory.RAM).setDetail(Map.of(
                "type", "DDR4",
                "speed_mhz", 3200,
                "capacity_gb", 8,
                "sticks", 2,
                "total_gb", 16
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Loại RAM (DDR4) không tương thích")));
    }

    @Test
    void checkProducts_MainboardFormFactorNotSupportedByCase_ReturnsViolation() {
        validComponents.get(ProductCategory.MAINBOARD).setDetail(Map.of(
                "socket", "LGA1700",
                "form_factor", "ATX",
                "chipset", "Z790",
                "memory_type", "DDR5",
                "memory_slots", 4,
                "max_memory_gb", 192,
                "m2_slots", 4,
                "pcie_version", "5.0"
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Kích thước Mainboard (ATX)")));
    }

    @Test
    void checkProducts_GpuLengthExceedsCaseMax_ReturnsViolation() {
        validComponents.get(ProductCategory.GPU).setDetail(Map.of(
                "vram_gb", 16,
                "vram_type", "GDDR6X",
                "tdp_w", 200,
                "pcie_slot", "x16",
                "length_mm", 358
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Chiều dài GPU (358mm)")));
    }

    @Test
    void checkProducts_CoolerHeightExceedsCaseMax_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU_COOLER).setDetail(Map.of(
                "type", "Air",
                "socket_support", List.of("LGA1700", "AM5"),
                "height_mm", 175,
                "tdp_support_w", 250
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Chiều cao tản nhiệt CPU (175mm)")));
    }

    @Test
    void checkProducts_CoolerSocketNotSupportingCpuSocket_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU_COOLER).setDetail(Map.of(
                "type", "Air",
                "socket_support", List.of("AM5", "AM4"),
                "height_mm", 155,
                "tdp_support_w", 220
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Tản nhiệt CPU không hỗ trợ socket của CPU")));
    }

    @Test
    void checkProducts_TotalTdpExceedsPsuSafeLimit_ReturnsViolation() {
        // PSU 650W * 0.8 = 520W safe limit. Set CPU TDP 250W + GPU TDP 320W = 570W > 520W
        validComponents.get(ProductCategory.CPU).setDetail(Map.of(
                "socket", "LGA1700",
                "cores", 24,
                "threads", 32,
                "base_clock_ghz", 3.2,
                "boost_clock_ghz", 6.0,
                "tdp_w", 250,
                "memory_type", "DDR5",
                "integrated_gpu", true
        ));
        validComponents.get(ProductCategory.GPU).setDetail(Map.of(
                "vram_gb", 16,
                "vram_type", "GDDR6X",
                "tdp_w", 320,
                "pcie_slot", "x16",
                "length_mm", 300
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Tổng TDP của CPU và GPU (570W)")));
    }

    @Test
    void checkProducts_ComponentOutOfStock_ReturnsViolation() {
        validComponents.get(ProductCategory.GPU).setStockQuantity(0);

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("đã hết hàng trong kho")));
    }

    private Product buildProduct(ProductCategory category, String name, int stock, Map<String, Object> detail) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .price(new BigDecimal("2000000.00"))
                .stockQuantity(stock)
                .category(category)
                .detail(detail)
                .isActive(true)
                .build();
    }
}
