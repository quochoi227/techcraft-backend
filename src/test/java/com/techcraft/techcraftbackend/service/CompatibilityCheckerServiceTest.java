package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.service.impl.CompatibilityCheckerServiceImpl;
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

    private CompatibilityCheckerServiceImpl compatibilityCheckerService;

    private Map<ProductCategory, Product> validComponents;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        compatibilityCheckerService = new CompatibilityCheckerServiceImpl(productRepository, objectMapper);

        validComponents = new EnumMap<>(ProductCategory.class);
        validComponents.put(ProductCategory.CPU, buildProduct(ProductCategory.CPU, "Intel Core i5-13400F", 10, Map.of(
                "socket", "LGA1700",
                "cores", 10,
                "threads", 16,
                "baseClockGhz", 2.5,
                "boostClockGhz", 4.6,
                "tdpW", 65,
                "memoryType", "DDR4/DDR5",
                "integratedGpu", false
        )));
        validComponents.put(ProductCategory.MAINBOARD, buildProduct(ProductCategory.MAINBOARD, "MSI MAG B760M MORTAR WIFI II", 10, Map.of(
                "socket", "LGA1700",
                "formFactor", "mATX",
                "chipset", "B760",
                "memoryType", "DDR5",
                "memorySlots", 4,
                "maxMemoryGb", 256,
                "m2Slots", 3,
                "pcieVersion", "5.0"
        )));
        validComponents.put(ProductCategory.RAM, buildProduct(ProductCategory.RAM, "Corsair Vengeance RGB 32GB DDR5", 10, Map.of(
                "type", "DDR5",
                "speedMhz", 6000,
                "capacityGb", 16,
                "sticks", 2,
                "totalGb", 32
        )));
        validComponents.put(ProductCategory.GPU, buildProduct(ProductCategory.GPU, "MSI RTX 4060 VENTUS 2X", 10, Map.of(
                "vramGb", 8,
                "vramType", "GDDR6",
                "tdpW", 115,
                "pcieSlot", "x16",
                "lengthMm", 199
        )));
        validComponents.put(ProductCategory.STORAGE, buildProduct(ProductCategory.STORAGE, "Samsung 990 PRO 1TB", 10, Map.of(
                "type", "NVMe SSD",
                "capacityGb", 1000,
                "interface", "M.2 PCIe 4.0",
                "readMbps", 7450,
                "writeMbps", 6900
        )));
        validComponents.put(ProductCategory.PSU, buildProduct(ProductCategory.PSU, "MSI MAG A650BN 650W", 10, Map.of(
                "wattageW", 650,
                "efficiency", "80+ Bronze",
                "modular", "Non-modular",
                "formFactor", "ATX"
        )));
        validComponents.put(ProductCategory.CASE, buildProduct(ProductCategory.CASE, "ASUS Prime AP201", 10, Map.of(
                "formFactorSupport", List.of("mATX", "ITX"),
                "maxGpuLengthMm", 338,
                "maxCoolerHeightMm", 170,
                "driveBays35", 3,
                "driveBays25", 4
        )));
        validComponents.put(ProductCategory.CPU_COOLER, buildProduct(ProductCategory.CPU_COOLER, "DeepCool AK400 Digital", 10, Map.of(
                "type", "Air",
                "socketSupport", List.of("LGA1700", "AM5", "AM4"),
                "heightMm", 156,
                "tdpSupportW", 220
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
                "baseClockGhz", 4.7,
                "boostClockGhz", 5.3,
                "tdpW", 105,
                "memoryType", "DDR5",
                "integratedGpu", true
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
                "baseClockGhz", 2.5,
                "boostClockGhz", 4.6,
                "tdpW", 65,
                "memoryType", "DDR4",
                "integratedGpu", false
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Loại RAM hỗ trợ của CPU")));
    }

    @Test
    void checkProducts_RamAndMainboardMemoryTypeMismatch_ReturnsViolation() {
        validComponents.get(ProductCategory.RAM).setDetail(Map.of(
                "type", "DDR4",
                "speedMhz", 3200,
                "capacityGb", 8,
                "sticks", 2,
                "totalGb", 16
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Loại RAM (DDR4) không tương thích")));
    }

    @Test
    void checkProducts_MainboardFormFactorNotSupportedByCase_ReturnsViolation() {
        validComponents.get(ProductCategory.MAINBOARD).setDetail(Map.of(
                "socket", "LGA1700",
                "formFactor", "ATX",
                "chipset", "Z790",
                "memoryType", "DDR5",
                "memorySlots", 4,
                "maxMemoryGb", 192,
                "m2Slots", 4,
                "pcieVersion", "5.0"
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Kích thước Mainboard (ATX)")));
    }

    @Test
    void checkProducts_GpuLengthExceedsCaseMax_ReturnsViolation() {
        validComponents.get(ProductCategory.GPU).setDetail(Map.of(
                "vramGb", 16,
                "vramType", "GDDR6X",
                "tdpW", 200,
                "pcieSlot", "x16",
                "lengthMm", 358
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Chiều dài GPU (358mm)")));
    }

    @Test
    void checkProducts_CoolerHeightExceedsCaseMax_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU_COOLER).setDetail(Map.of(
                "type", "Air",
                "socketSupport", List.of("LGA1700", "AM5"),
                "heightMm", 175,
                "tdpSupportW", 250
        ));

        CompatibilityResult result = compatibilityCheckerService.checkProducts(validComponents);

        assertFalse(result.isCompatible());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("Chiều cao tản nhiệt CPU (175mm)")));
    }

    @Test
    void checkProducts_CoolerSocketNotSupportingCpuSocket_ReturnsViolation() {
        validComponents.get(ProductCategory.CPU_COOLER).setDetail(Map.of(
                "type", "Air",
                "socketSupport", List.of("AM5", "AM4"),
                "heightMm", 155,
                "tdpSupportW", 220
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
                "baseClockGhz", 3.2,
                "boostClockGhz", 6.0,
                "tdpW", 250,
                "memoryType", "DDR5",
                "integratedGpu", true
        ));
        validComponents.get(ProductCategory.GPU).setDetail(Map.of(
                "vramGb", 16,
                "vramType", "GDDR6X",
                "tdpW", 320,
                "pcieSlot", "x16",
                "lengthMm", 300
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

    @Test
    void resolveProductsByCategory_ValidAndInvalidItems_PopulatesMapAndViolations() {
        Product cpu = validComponents.get(ProductCategory.CPU);
        UUID missingId = UUID.randomUUID();

        org.mockito.Mockito.when(productRepository.findAllById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(cpu));

        List<com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest> itemRequests = List.of(
                com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest.builder()
                        .category(ProductCategory.CPU)
                        .productId(cpu.getId())
                        .build(),
                com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest.builder()
                        .category(ProductCategory.GPU)
                        .productId(missingId)
                        .build()
        );

        List<String> violations = new java.util.ArrayList<>();
        Map<ProductCategory, Product> resolved =
                compatibilityCheckerService.resolveProductsByCategory(itemRequests, violations);

        assertEquals(1, resolved.size());
        assertEquals(cpu, resolved.get(ProductCategory.CPU));
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains(missingId.toString()));
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
