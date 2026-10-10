package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.ProductFilterRequest;
import com.techcraft.techcraftbackend.dto.request.ProductImageRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProductRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.ProductMapper;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.validator.ProductDetailValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    @Test
    void getProducts_DefaultFilter_Success() {
        Product entity = Product.builder()
                .id(UUID.randomUUID())
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .stockQuantity(entity.getStockQuantity())
                .category(entity.getCategory())
                .isActive(true)
                .build();

        Page<Product> page = new PageImpl<>(List.of(entity));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(entity)).thenReturn(responseDto);

        ProductFilterRequest filter = new ProductFilterRequest();
        PageResponse<ProductResponse> result = productService.getProducts(filter);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Intel Core i7-13700K", result.getContent().get(0).getName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(12, captured.getPageSize());
        assertEquals(Sort.Direction.DESC, captured.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getProducts_WithCustomFilterAndSorting_Success() {
        Product entity = Product.builder()
                .id(UUID.randomUUID())
                .name("NVIDIA RTX 4070")
                .price(new BigDecimal("16000000.00"))
                .stockQuantity(5)
                .category(ProductCategory.GPU)
                .isActive(true)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .category(entity.getCategory())
                .isActive(true)
                .build();

        Page<Product> page = new PageImpl<>(List.of(entity));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(entity)).thenReturn(responseDto);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .keyword("rtx")
                .category(ProductCategory.GPU)
                .minPrice(new BigDecimal("10000000.00"))
                .maxPrice(new BigDecimal("20000000.00"))
                .inStock(true)
                .page(1)
                .size(5)
                .sortBy("price")
                .sortDir("asc")
                .build();

        PageResponse<ProductResponse> result = productService.getProducts(filter);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("NVIDIA RTX 4070", result.getContent().get(0).getName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());
        assertEquals(Sort.Direction.ASC, captured.getSort().getOrderFor("price").getDirection());
    }

    @Test
    void getProducts_MinPriceGreaterThanMaxPrice_ThrowsBadRequestException() {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .minPrice(new BigDecimal("20000000.00"))
                .maxPrice(new BigDecimal("10000000.00"))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.getProducts(filter)
        );

        assertTrue(ex.getMessage().contains("không được lớn hơn"));
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProducts_NegativeMinPrice_ThrowsBadRequestException() {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .minPrice(new BigDecimal("-1000.00"))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.getProducts(filter)
        );

        assertTrue(ex.getMessage().contains("không được nhỏ hơn 0"));
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProducts_InvalidSortField_ThrowsBadRequestException() {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .sortBy("invalid_column")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.getProducts(filter)
        );

        assertTrue(ex.getMessage().contains("Trường sắp xếp không hợp lệ"));
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProducts_InvalidSortDir_ThrowsBadRequestException() {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .sortDir("sideways")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.getProducts(filter)
        );

        assertTrue(ex.getMessage().contains("Chiều sắp xếp (sortDir) không hợp lệ"));
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProducts_AsAdmin_AllowsAccess() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@techcraft.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .isActive(false)
                .build();

        PageResponse<ProductResponse> result = productService.getProducts(filter);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProductById_Success_ReturnsProductResponse() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(id)
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(responseDto);

        ProductResponse result = productService.getProductById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Intel Core i7-13700K", result.getName());
        verify(productRepository).findById(id);
    }

    @Test
    void getProductById_NotFound_ThrowsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductById(id)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy linh kiện"));
        verify(productRepository).findById(id);
    }

    @Test
    void getProductById_InactiveProduct_AsPublicUser_ThrowsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .name("Hidden Product")
                .isActive(false)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductById(id)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy linh kiện"));
        verify(productMapper, never()).toResponse(any());
    }

    @Test
    void getProductById_InactiveProduct_AsAdmin_Success() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@techcraft.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .name("Hidden Product")
                .isActive(false)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(id)
                .name("Hidden Product")
                .isActive(false)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(responseDto);

        ProductResponse result = productService.getProductById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Hidden Product", result.getName());
        assertFalse(result.isActive());
        verify(productRepository).findById(id);
    }

    @Test
    void updateProduct_Success_WithNewImages() {
        UUID id = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(id)
                .name("Old Product Name")
                .price(new BigDecimal("5000000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Core i7-13700K")
                .price(new BigDecimal("10900000.00"))
                .stockQuantity(25)
                .category(ProductCategory.CPU)
                .description("Updated description")
                .detail(sampleDetail)
                .isActive(false)
                .images(List.of(
                        ProductImageRequest.builder().imageUrl("https://example.com/new1.jpg").isPrimary(true).displayOrder(0).build(),
                        ProductImageRequest.builder().imageUrl("https://example.com/new2.jpg").isPrimary(false).displayOrder(1).build()
                ))
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Updated Core i7-13700K", id)).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(eq(ProductCategory.CPU), any())).thenReturn(sampleDetail);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse expectedResponse = ProductResponse.builder()
                .id(id)
                .name(updateRequest.getName())
                .price(updateRequest.getPrice())
                .stockQuantity(updateRequest.getStockQuantity())
                .category(updateRequest.getCategory())
                .detail(sampleDetail)
                .isActive(false)
                .build();
        when(productMapper.toResponse(any(Product.class))).thenReturn(expectedResponse);

        ProductResponse actualResponse = productService.updateProduct(id, updateRequest);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getName(), actualResponse.getName());
        assertFalse(actualResponse.isActive());
        verify(productMapper).updateEntityFromRequest(eq(updateRequest), eq(existingProduct));
        verify(productRepository).save(argThat(p ->
                p.getName().equals("Updated Core i7-13700K") &&
                !p.isActive() &&
                p.getImages().size() == 2 &&
                p.getImages().get(0).isPrimary() &&
                !p.getImages().get(1).isPrimary()
        ));
    }

    @Test
    void updateProduct_Success_KeepExistingImages_WhenImagesNull() {
        UUID id = UUID.randomUUID();
        ProductImage existingImage = ProductImage.builder()
                .id(UUID.randomUUID())
                .imageUrl("https://example.com/existing.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        Product existingProduct = Product.builder()
                .id(id)
                .name("Old Product Name")
                .price(new BigDecimal("5000000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .isActive(true)
                .images(new ArrayList<>(List.of(existingImage)))
                .build();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Name")
                .price(new BigDecimal("6000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .images(null) // null images -> keep existing
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot(anyString(), eq(id))).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(any(), any())).thenReturn(sampleDetail);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().build());

        productService.updateProduct(id, updateRequest);

        verify(productRepository).save(argThat(p ->
                p.getImages().size() == 1 &&
                p.getImages().get(0).getImageUrl().equals("https://example.com/existing.jpg")
        ));
    }

    @Test
    void updateProduct_NotFound_ThrowsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Any Name")
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                productService.updateProduct(id, updateRequest)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy linh kiện với ID: " + id));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_DuplicateNameWithAnotherProduct_ThrowsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(id)
                .name("Old Product Name")
                .build();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Existed Name In System")
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Existed Name In System", id)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                productService.updateProduct(id, updateRequest)
        );

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(productDetailValidator, never()).validateAndNormalize(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_SameNameCurrentProduct_Success() {
        UUID id = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(id)
                .name("Current Name")
                .price(new BigDecimal("5000000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Current Name")
                .price(new BigDecimal("5500000.00"))
                .stockQuantity(12)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Current Name", id)).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(any(), any())).thenReturn(sampleDetail);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().build());

        assertDoesNotThrow(() -> productService.updateProduct(id, updateRequest));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_MultiplePrimaryImages_ThrowsBadRequestException() {
        UUID id = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(id)
                .name("Old Name")
                .images(new ArrayList<>())
                .build();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Name")
                .price(new BigDecimal("5000000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(sampleDetail)
                .images(List.of(
                        ProductImageRequest.builder().imageUrl("https://example.com/img1.jpg").isPrimary(true).build(),
                        ProductImageRequest.builder().imageUrl("https://example.com/img2.jpg").isPrimary(true).build()
                ))
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Updated Name", id)).thenReturn(false);
        when(productDetailValidator.validateAndNormalize(any(), any())).thenReturn(sampleDetail);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                productService.updateProduct(id, updateRequest)
        );

        assertTrue(ex.getMessage().contains("tối đa 1 ảnh"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_Success_SoftDeletesProduct() {
        UUID id = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(id)
                .name("Intel Core i7-13700K")
                .isActive(true)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.deleteProduct(id));

        assertFalse(existingProduct.isActive());
        verify(productRepository).save(argThat(p -> p.getId().equals(id) && !p.isActive()));
    }

    @Test
    void deleteProduct_NotFound_ThrowsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                productService.deleteProduct(id)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy linh kiện với ID: " + id));
        verify(productRepository, never()).save(any());
    }
}
