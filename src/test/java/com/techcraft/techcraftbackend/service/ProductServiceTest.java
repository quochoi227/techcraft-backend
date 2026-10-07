package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.ProductImageRequest;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.mapper.ProductMapper;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.validator.ProductDetailValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDetailValidator productDetailValidator;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private CreateProductRequest validRequest;
    private Map<String, Object> sampleDetail;

    @BeforeEach
    void setUp() {
        sampleDetail = Map.of(
                "socket", "LGA1700",
                "cores", 16,
                "threads", 24,
                "base_clock_ghz", 3.0,
                "boost_clock_ghz", 5.4,
                "tdp_w", 125,
                "memory_type", "DDR5",
                "integrated_gpu", true
        );

        validRequest = CreateProductRequest.builder()
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(20)
                .category(ProductCategory.CPU)
                .description("CPU Intel thế hệ 13")
                .detail(sampleDetail)
                .isActive(true)
                .images(new ArrayList<>(List.of(
                        ProductImageRequest.builder().imageUrl("https://example.com/img1.jpg").isPrimary(false).displayOrder(0).build(),
                        ProductImageRequest.builder().imageUrl("https://example.com/img2.jpg").isPrimary(true).displayOrder(1).build()
                )))
                .build();
    }

    @Test
    void createProduct_Success_WithExplicitPrimaryImage() {
        when(productRepository.existsByNameIgnoreCase("Intel Core i7-13700K")).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(eq(ProductCategory.CPU), any())).thenReturn(sampleDetail);

        Product entity = Product.builder()
                .name("Intel Core i7-13700K")
                .price(validRequest.getPrice())
                .stockQuantity(validRequest.getStockQuantity())
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .build();

        when(productMapper.toEntity(validRequest)).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ProductResponse expectedResponse = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name(validRequest.getName())
                .price(validRequest.getPrice())
                .stockQuantity(validRequest.getStockQuantity())
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .isActive(true)
                .build();

        when(productMapper.toResponse(any(Product.class))).thenReturn(expectedResponse);

        ProductResponse actualResponse = productService.createProduct(validRequest);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getName(), actualResponse.getName());
        verify(productRepository).save(argThat(p ->
                p.getImages().size() == 2 &&
                !p.getImages().get(0).isPrimary() &&
                p.getImages().get(1).isPrimary()
        ));
    }

    @Test
    void createProduct_Success_AutoAssignsFirstImagePrimary_WhenNoneSpecified() {
        validRequest.setImages(List.of(
                ProductImageRequest.builder().imageUrl("https://example.com/img1.jpg").isPrimary(false).displayOrder(0).build(),
                ProductImageRequest.builder().imageUrl("https://example.com/img2.jpg").isPrimary(false).displayOrder(1).build()
        ));

        when(productRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(any(), any())).thenReturn(sampleDetail);

        Product entity = Product.builder().build();
        when(productMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().build());

        productService.createProduct(validRequest);

        verify(productRepository).save(argThat(p ->
                p.getImages().size() == 2 &&
                p.getImages().get(0).isPrimary() &&
                !p.getImages().get(1).isPrimary()
        ));
    }

    @Test
    void createProduct_MultiplePrimaryImages_ThrowsBadRequestException() {
        validRequest.setImages(List.of(
                ProductImageRequest.builder().imageUrl("https://example.com/img1.jpg").isPrimary(true).build(),
                ProductImageRequest.builder().imageUrl("https://example.com/img2.jpg").isPrimary(true).build()
        ));

        when(productRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(any(), any())).thenReturn(sampleDetail);
        when(productMapper.toEntity(any())).thenReturn(Product.builder().build());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.createProduct(validRequest)
        );

        assertTrue(ex.getMessage().contains("tối đa 1 ảnh"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_DuplicateName_ThrowsDuplicateResourceException() {
        when(productRepository.existsByNameIgnoreCase(validRequest.getName())).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                productService.createProduct(validRequest)
        );

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(productDetailValidator, never()).validateAndNormalize(any(), any());
        verify(productRepository, never()).save(any());
    }
}
