package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.mapper.PcCaseMapper;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import com.techcraft.techcraftbackend.service.impl.PcConfigSuggestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcConfigSuggestionServiceTest {

    @Mock
    private PcCaseRepository pcCaseRepository;

    @Mock
    private PcCaseMapper pcCaseMapper;

    private PcConfigSuggestionService pcConfigSuggestionService;

    @BeforeEach
    void setUp() {
        pcConfigSuggestionService = new PcConfigSuggestionServiceImpl(pcCaseRepository, pcCaseMapper);
    }

    @Test
    void getAvailablePcCasesPromptText_OnlyIncludesInStockCases() {
        UUID inStockId = UUID.randomUUID();
        UUID outOfStockId = UUID.randomUUID();

        Product cpuInStock = Product.builder().name("Intel i5").price(BigDecimal.valueOf(4000000)).stockQuantity(5).isActive(true).detail(Map.of("socket", "LGA1700")).build();
        Product gpuInStock = Product.builder().name("RTX 4060").price(BigDecimal.valueOf(8000000)).stockQuantity(2).isActive(true).detail(Map.of("vram_gb", 8)).build();

        PcCase validCase = PcCase.builder()
                .id(inStockId)
                .name("PC Gaming Valid")
                .useCase("gaming")
                .totalPrice(BigDecimal.valueOf(12000000))
                .description("Cấu hình chuẩn")
                .isActive(true)
                .items(new ArrayList<>(List.of(
                        PcCaseItem.builder().category(ProductCategory.CPU).product(cpuInStock).build(),
                        PcCaseItem.builder().category(ProductCategory.GPU).product(gpuInStock).build()
                )))
                .build();

        Product cpuOutOfStock = Product.builder().name("Intel i9").price(BigDecimal.valueOf(15000000)).stockQuantity(0).isActive(true).detail(Map.of("socket", "LGA1700")).build();
        PcCase outOfStockCase = PcCase.builder()
                .id(outOfStockId)
                .name("PC Gaming OutOfStock")
                .useCase("gaming")
                .totalPrice(BigDecimal.valueOf(25000000))
                .isActive(true)
                .items(new ArrayList<>(List.of(
                        PcCaseItem.builder().category(ProductCategory.CPU).product(cpuOutOfStock).build()
                )))
                .build();

        when(pcCaseRepository.findAll()).thenReturn(List.of(validCase, outOfStockCase));

        String promptText = pcConfigSuggestionService.getAvailablePcCasesPromptText();

        assertTrue(promptText.contains(inStockId.toString()));
        assertTrue(promptText.contains("PC Gaming Valid"));
        assertFalse(promptText.contains(outOfStockId.toString()));
        assertFalse(promptText.contains("PC Gaming OutOfStock"));
        // Đảm bảo không chứa các trường không cần thiết như images hay reviews
        assertFalse(promptText.contains("image_url"));
        assertFalse(promptText.contains("reviews"));
    }

    @Test
    void extractPcCaseIds_FromTagAndUuid_ReturnsCorrectSet() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        String responseText = "Tôi gợi ý cấu hình [PC_CASE_ID: " + id1 + "] phù hợp nhất với ngân sách của bạn. Hoặc bạn có thể xem xét thêm cấu hình thứ hai là " + id2 + ".";

        Set<UUID> ids = pcConfigSuggestionService.extractPcCaseIds(responseText);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }
}
