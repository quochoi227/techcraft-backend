package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.ProductImageRequest;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.mapper.ProductMapper;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.validator.ProductDetailValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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

        processImages(product, request);

        Product savedProduct = productRepository.save(product);
        log.info("Created new product successfully with id: {}, name: {}, category: {}",
                savedProduct.getId(), savedProduct.getName(), savedProduct.getCategory());

        return productMapper.toResponse(savedProduct);
    }

    private void processImages(Product product, CreateProductRequest request) {
        if (request.getImages() == null || request.getImages().isEmpty()) {
            return;
        }

        long primaryCount = request.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .count();

        if (primaryCount > 1) {
            throw new BadRequestException("Chỉ được phép chọn tối đa 1 ảnh làm ảnh đại diện (isPrimary = true)");
        }

        boolean hasPrimary = primaryCount == 1;

        for (int i = 0; i < request.getImages().size(); i++) {
            ProductImageRequest imgReq = request.getImages().get(i);
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
}
