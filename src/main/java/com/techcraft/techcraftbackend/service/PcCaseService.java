package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.mapper.PcCaseMapper;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PcCaseService {

    private final PcCaseRepository pcCaseRepository;
    private final CompatibilityChecker compatibilityChecker;
    private final PcCaseMapper pcCaseMapper;

    @Transactional
    public PcCaseResponse createPcCase(CreatePcCaseRequest request) {
        String trimmedName = request.getName().trim();
        if (pcCaseRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Cấu hình PC với tên '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        List<PcCaseItemRequest> itemRequests = request.resolveItems();
        if (itemRequests.isEmpty()) {
            throw new BadRequestException("Danh sách linh kiện của cấu hình PC không được để trống");
        }

        List<String> violations = new ArrayList<>();
        Map<ProductCategory, Product> componentsByCategory =
                compatibilityChecker.resolveProductsByCategory(itemRequests, violations);

        CompatibilityResult productCheckResult = compatibilityChecker.checkProducts(componentsByCategory);
        violations.addAll(productCheckResult.getViolations());

        if (!violations.isEmpty()) {
            CompatibilityResult failedResult = CompatibilityResult.fail(violations);
            throw new BadRequestException(
                    "Cấu hình PC không tương thích: " + String.join("; ", violations),
                    failedResult
            );
        }

        BigDecimal totalPrice = componentsByCategory.values().stream()
                .map(Product::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PcCase pcCase = PcCase.builder()
                .name(trimmedName)
                .useCase(StringUtils.hasText(request.getUseCase()) ? request.getUseCase().trim() : null)
                .description(request.getDescription())
                .totalPrice(totalPrice)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        for (ProductCategory category : ProductCategory.values()) {
            Product product = componentsByCategory.get(category);
            if (product != null) {
                PcCaseItem item = PcCaseItem.builder()
                        .category(category)
                        .product(product)
                        .build();
                pcCase.addItem(item);
            }
        }

        PcCase savedPcCase = pcCaseRepository.save(pcCase);
        log.info("Created new PC Case successfully with id: {}, name: {}, totalPrice: {}",
                savedPcCase.getId(), savedPcCase.getName(), savedPcCase.getTotalPrice());
        log.info("PC Case created at: {}, updated at: {}", savedPcCase.getCreatedAt(), savedPcCase.getUpdatedAt());

        return pcCaseMapper.toResponse(savedPcCase);
    }

    @Transactional(readOnly = true)
    public CompatibilityResult validatePcCase(ValidatePcCaseRequest request) {
        return compatibilityChecker.check(request);
    }
}
