package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.PcCaseMapper;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.techcraft.techcraftbackend.service.impl.PcCaseServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PcCaseServiceTest {

    @Mock
    private PcCaseRepository pcCaseRepository;

    @Mock
    private CompatibilityChecker compatibilityChecker;

    @Mock
    private PcCaseMapper pcCaseMapper;

    @InjectMocks
    private PcCaseServiceImpl pcCaseService;

    private Map<ProductCategory, Product> sampleProductsByCategory;
    private List<PcCaseItemRequest> sampleItemRequests;

    @BeforeEach
    void setUp() {
        sampleProductsByCategory = new EnumMap<>(ProductCategory.class);
        sampleItemRequests = new ArrayList<>();

        for (ProductCategory category : ProductCategory.values()) {
            UUID id = UUID.randomUUID();
            Product product = Product.builder()
                    .id(id)
                    .name("Sample " + category)
                    .price(new BigDecimal("2500000.00"))
                    .stockQuantity(10)
                    .category(category)
                    .detail(Map.of())
                    .isActive(true)
                    .build();
            sampleProductsByCategory.put(category, product);
            sampleItemRequests.add(PcCaseItemRequest.builder()
                    .category(category)
                    .productId(id)
                    .build());
        }
    }

    @Test
    void createPcCase_ValidRequest_CalculatesTotalPriceAndSaves() {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .description("PC Gaming tầm trung chiến mượt Full HD")
                .isActive(true)
                .items(sampleItemRequests)
                .build();

        when(pcCaseRepository.existsByNameIgnoreCase("Gaming Entry 2025")).thenReturn(false);
        when(compatibilityChecker.resolveProductsByCategory(any(), any())).thenReturn(sampleProductsByCategory);
        when(compatibilityChecker.checkProducts(any())).thenReturn(CompatibilityResult.ok());
        when(pcCaseRepository.save(any(PcCase.class))).thenAnswer(inv -> {
            PcCase saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        BigDecimal expectedTotal = new BigDecimal("20000000.00"); // 8 * 2,500,000
        PcCaseResponse expectedResponse = PcCaseResponse.builder()
                .id(UUID.randomUUID())
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .totalPrice(expectedTotal)
                .isActive(true)
                .build();
        when(pcCaseMapper.toResponse(any(PcCase.class))).thenReturn(expectedResponse);

        PcCaseResponse actual = pcCaseService.createPcCase(request);

        assertNotNull(actual);
        assertEquals("Gaming Entry 2025", actual.getName());
        assertEquals(0, expectedTotal.compareTo(actual.getTotalPrice()));

        ArgumentCaptor<PcCase> captor = ArgumentCaptor.forClass(PcCase.class);
        verify(pcCaseRepository).save(captor.capture());
        PcCase savedEntity = captor.getValue();
        assertEquals("Gaming Entry 2025", savedEntity.getName());
        assertEquals("gaming", savedEntity.getUseCase());
        assertEquals(0, expectedTotal.compareTo(savedEntity.getTotalPrice()));
        assertEquals(8, savedEntity.getItems().size());
        assertTrue(savedEntity.isActive());
    }

    @Test
    void createPcCase_DuplicateName_ThrowsDuplicateResourceException() {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .items(sampleItemRequests)
                .build();

        when(pcCaseRepository.existsByNameIgnoreCase("Gaming Entry 2025")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                pcCaseService.createPcCase(request)
        );

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void createPcCase_EmptyItems_ThrowsBadRequestException() {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Empty PC")
                .items(List.of())
                .build();

        when(pcCaseRepository.existsByNameIgnoreCase("Empty PC")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pcCaseService.createPcCase(request)
        );

        assertTrue(ex.getMessage().contains("không được để trống"));
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void createPcCase_IncompatibleComponents_ThrowsBadRequestExceptionWithCompatibilityResult() {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Incompatible PC")
                .items(sampleItemRequests)
                .build();

        when(pcCaseRepository.existsByNameIgnoreCase("Incompatible PC")).thenReturn(false);
        when(compatibilityChecker.resolveProductsByCategory(any(), any())).thenReturn(sampleProductsByCategory);
        when(compatibilityChecker.checkProducts(any())).thenReturn(
                CompatibilityResult.fail(List.of("Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)"))
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pcCaseService.createPcCase(request)
        );

        assertTrue(ex.getMessage().contains("Socket của CPU (AM5)"));
        assertNotNull(ex.getData());
        assertInstanceOf(CompatibilityResult.class, ex.getData());
        CompatibilityResult result = (CompatibilityResult) ex.getData();
        assertFalse(result.isCompatible());
        assertEquals(1, result.getViolations().size());
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void updatePcCase_ValidRequestWithNewItems_CalculatesNewPriceAndSaves() {
        UUID caseId = UUID.randomUUID();
        PcCase existingCase = PcCase.builder()
                .id(caseId)
                .name("Old Gaming PC")
                .useCase("office")
                .totalPrice(new BigDecimal("10000000.00"))
                .description("Old description")
                .isActive(true)
                .build();

        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Updated Gaming PC 2026")
                .useCase("gaming")
                .description("Cấu hình nâng cấp chiến game mượt mà")
                .isActive(false)
                .items(sampleItemRequests)
                .build();

        when(pcCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(pcCaseRepository.existsByNameIgnoreCaseAndIdNot("Updated Gaming PC 2026", caseId)).thenReturn(false);
        when(compatibilityChecker.resolveProductsByCategory(any(), any())).thenReturn(sampleProductsByCategory);
        when(compatibilityChecker.checkProducts(any())).thenReturn(CompatibilityResult.ok());
        when(pcCaseRepository.save(any(PcCase.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal expectedTotal = new BigDecimal("20000000.00");
        PcCaseResponse expectedResponse = PcCaseResponse.builder()
                .id(caseId)
                .name("Updated Gaming PC 2026")
                .useCase("gaming")
                .totalPrice(expectedTotal)
                .isActive(false)
                .build();
        when(pcCaseMapper.toResponse(any(PcCase.class))).thenReturn(expectedResponse);

        PcCaseResponse actual = pcCaseService.updatePcCase(caseId, request);

        assertNotNull(actual);
        assertEquals("Updated Gaming PC 2026", actual.getName());
        assertEquals(0, expectedTotal.compareTo(actual.getTotalPrice()));

        ArgumentCaptor<PcCase> captor = ArgumentCaptor.forClass(PcCase.class);
        verify(pcCaseRepository).save(captor.capture());
        PcCase savedEntity = captor.getValue();
        assertEquals("Updated Gaming PC 2026", savedEntity.getName());
        assertEquals("gaming", savedEntity.getUseCase());
        assertEquals(0, expectedTotal.compareTo(savedEntity.getTotalPrice()));
        assertEquals(8, savedEntity.getItems().size());
        assertFalse(savedEntity.isActive());
    }

    @Test
    void updatePcCase_ValidRequestWithoutItems_KeepsExistingItemsAndPrice() {
        UUID caseId = UUID.randomUUID();
        BigDecimal existingPrice = new BigDecimal("15000000.00");
        PcCase existingCase = PcCase.builder()
                .id(caseId)
                .name("Original Name")
                .useCase("study")
                .totalPrice(existingPrice)
                .description("Original description")
                .isActive(true)
                .build();

        // Add 1 sample item to existingCase
        existingCase.addItem(PcCaseItem.builder()
                .category(ProductCategory.CPU)
                .product(sampleProductsByCategory.get(ProductCategory.CPU))
                .build());

        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("New Name Only")
                .useCase("office")
                .description("New description")
                .isActive(true)
                .items(null)
                .productIds(null)
                .components(null)
                .build();

        when(pcCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(pcCaseRepository.existsByNameIgnoreCaseAndIdNot("New Name Only", caseId)).thenReturn(false);
        when(pcCaseRepository.save(any(PcCase.class))).thenAnswer(inv -> inv.getArgument(0));

        PcCaseResponse expectedResponse = PcCaseResponse.builder()
                .id(caseId)
                .name("New Name Only")
                .totalPrice(existingPrice)
                .build();
        when(pcCaseMapper.toResponse(any(PcCase.class))).thenReturn(expectedResponse);

        PcCaseResponse actual = pcCaseService.updatePcCase(caseId, request);

        assertNotNull(actual);
        assertEquals("New Name Only", actual.getName());
        assertEquals(0, existingPrice.compareTo(actual.getTotalPrice()));

        ArgumentCaptor<PcCase> captor = ArgumentCaptor.forClass(PcCase.class);
        verify(pcCaseRepository).save(captor.capture());
        PcCase savedEntity = captor.getValue();
        assertEquals("New Name Only", savedEntity.getName());
        assertEquals("office", savedEntity.getUseCase());
        assertEquals(0, existingPrice.compareTo(savedEntity.getTotalPrice()));
        assertEquals(1, savedEntity.getItems().size());

        verify(compatibilityChecker, never()).resolveProductsByCategory(any(), any());
        verify(compatibilityChecker, never()).checkProducts(any());
    }

    @Test
    void updatePcCase_NotFound_ThrowsResourceNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Updated Name")
                .build();

        when(pcCaseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                pcCaseService.updatePcCase(nonExistentId, request)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy cấu hình PC"));
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void updatePcCase_DuplicateName_ThrowsDuplicateResourceException() {
        UUID caseId = UUID.randomUUID();
        PcCase existingCase = PcCase.builder().id(caseId).name("Existing PC").build();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Duplicate Name")
                .build();

        when(pcCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(pcCaseRepository.existsByNameIgnoreCaseAndIdNot("Duplicate Name", caseId)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                pcCaseService.updatePcCase(caseId, request)
        );

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void updatePcCase_EmptyItemsList_ThrowsBadRequestException() {
        UUID caseId = UUID.randomUUID();
        PcCase existingCase = PcCase.builder().id(caseId).name("Existing PC").build();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("PC With Empty Items")
                .items(List.of())
                .build();

        when(pcCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(pcCaseRepository.existsByNameIgnoreCaseAndIdNot("PC With Empty Items", caseId)).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pcCaseService.updatePcCase(caseId, request)
        );

        assertTrue(ex.getMessage().contains("không được để trống"));
        verify(pcCaseRepository, never()).save(any());
    }

    @Test
    void updatePcCase_IncompatibleComponents_ThrowsBadRequestExceptionWithViolations() {
        UUID caseId = UUID.randomUUID();
        PcCase existingCase = PcCase.builder().id(caseId).name("Existing PC").build();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Updated With Incompatible")
                .items(sampleItemRequests)
                .build();

        when(pcCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(pcCaseRepository.existsByNameIgnoreCaseAndIdNot("Updated With Incompatible", caseId)).thenReturn(false);
        when(compatibilityChecker.resolveProductsByCategory(any(), any())).thenReturn(sampleProductsByCategory);
        when(compatibilityChecker.checkProducts(any())).thenReturn(
                CompatibilityResult.fail(List.of("RAM không tương thích với Mainboard"))
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pcCaseService.updatePcCase(caseId, request)
        );

        assertTrue(ex.getMessage().contains("RAM không tương thích với Mainboard"));
        assertNotNull(ex.getData());
        CompatibilityResult result = (CompatibilityResult) ex.getData();
        assertFalse(result.isCompatible());
        verify(pcCaseRepository, never()).save(any());
    }
}
