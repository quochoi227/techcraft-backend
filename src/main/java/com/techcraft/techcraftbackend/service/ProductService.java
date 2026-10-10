package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.ProductFilterRequest;
import com.techcraft.techcraftbackend.dto.request.ProductImageRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProductRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.ProductMapper;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.specification.ProductSpecification;
import com.techcraft.techcraftbackend.validator.ProductDetailValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDetailValidator productDetailValidator;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String trimmedName = request.getName().trim();
        if (productRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Linh kiện với tên '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        Map<String, Object> validatedDetail = productDetailValidator.validateAndNormalize(
                request.getCategory(),
                request.getDetail()
        );

        Product product = productMapper.toEntity(request);
        product.setName(trimmedName);
        product.setDetail(validatedDetail);
        product.setActive(request.getIsActive() == null || request.getIsActive());

        applyImages(product, request.getImages());

        Product savedProduct = productRepository.save(product);
        log.info("Created new product successfully with id: {}, name: {}, category: {}",
                savedProduct.getId(), savedProduct.getName(), savedProduct.getCategory());

        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id));

        String trimmedName = request.getName().trim();
        if (productRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Linh kiện với tên '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        Map<String, Object> validatedDetail = productDetailValidator.validateAndNormalize(
                request.getCategory(),
                request.getDetail()
        );

        productMapper.updateEntityFromRequest(request, product);
        product.setName(trimmedName);
        product.setDetail(validatedDetail);
        if (request.getIsActive() != null) {
            product.setActive(request.getIsActive());
        }

        if (request.getImages() != null) {
            product.getImages().clear();
            applyImages(product, request.getImages());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product successfully with id: {}, name: {}, category: {}",
                updatedProduct.getId(), updatedProduct.getName(), updatedProduct.getCategory());

        return productMapper.toResponse(updatedProduct);
    }

    private void applyImages(Product product, List<ProductImageRequest> imageRequests) {
        if (imageRequests == null || imageRequests.isEmpty()) {
            return;
        }

        long primaryCount = imageRequests.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .count();

        if (primaryCount > 1) {
            throw new BadRequestException("Chỉ được phép chọn tối đa 1 ảnh làm ảnh đại diện (isPrimary = true)");
        }

        boolean hasPrimary = primaryCount == 1;

        for (int i = 0; i < imageRequests.size(); i++) {
            ProductImageRequest imgReq = imageRequests.get(i);
            boolean isPrimary = hasPrimary ? Boolean.TRUE.equals(imgReq.getIsPrimary()) : (i == 0);
            int displayOrder = (imgReq.getDisplayOrder() != null) ? imgReq.getDisplayOrder() : i;

            ProductImage image = ProductImage.builder()
                    .imageUrl(imgReq.getImageUrl().trim())
                    .isPrimary(isPrimary)
                    .displayOrder(displayOrder)
                    .build();

            product.addImage(image);
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id));

        if (!product.isActive() && !isAdmin()) {
            throw new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id);
        }

        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductFilterRequest filter) {
        if (filter == null) {
            filter = new ProductFilterRequest();
        }
        validateFilter(filter);

        Boolean effectiveIsActive = resolveEffectiveIsActive(filter.getIsActive());

        Specification<Product> spec = ProductSpecification.filter(
                filter.getEffectiveSearch(),
                filter.getCategory(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getInStock(),
                effectiveIsActive
        );

        Pageable pageable = createPageable(filter);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return PageResponse.from(productPage, productMapper::toResponse);
    }

    private Boolean resolveEffectiveIsActive(Boolean requestedIsActive) {
        if (isAdmin()) {
            return requestedIsActive;
        }
        return true;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void validateFilter(ProductFilterRequest filter) {
        if (filter.getMinPrice() != null && filter.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá tối thiểu (minPrice) không được nhỏ hơn 0");
        }
        if (filter.getMaxPrice() != null && filter.getMaxPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá tối đa (maxPrice) không được nhỏ hơn 0");
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new BadRequestException("Giá tối thiểu (minPrice) không được lớn hơn giá tối đa (maxPrice)");
        }
        if (filter.getPage() < 0) {
            throw new BadRequestException("Số trang (page) phải lớn hơn hoặc bằng 0");
        }
        if (filter.getSize() < 1 || filter.getSize() > 100) {
            throw new BadRequestException("Kích thước trang (size) phải từ 1 đến 100");
        }
        if (StringUtils.hasText(filter.getSortDir())
                && !filter.getSortDir().equalsIgnoreCase("asc")
                && !filter.getSortDir().equalsIgnoreCase("desc")) {
            throw new BadRequestException("Chiều sắp xếp (sortDir) không hợp lệ: '" + filter.getSortDir() + "'. Chỉ chấp nhận 'asc' hoặc 'desc'");
        }
    }

    private Pageable createPageable(ProductFilterRequest filter) {
        String sortBy = resolveSortProperty(filter.getSortBy());
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(direction, sortBy));
    }

    private String resolveSortProperty(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "createdAt";
        }
        return switch (sortBy.trim()) {
            case "createdAt", "created_at" -> "createdAt";
            case "price" -> "price";
            case "name" -> "name";
            case "stockQuantity", "stock_quantity" -> "stockQuantity";
            case "updatedAt", "updated_at" -> "updatedAt";
            default -> throw new BadRequestException(
                    "Trường sắp xếp không hợp lệ: '" + sortBy + "'. Các trường hợp lệ: createdAt, price, name, stockQuantity, updatedAt"
            );
        };
    }
}
