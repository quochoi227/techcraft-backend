package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseFilterRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
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
import com.techcraft.techcraftbackend.service.CompatibilityChecker;
import com.techcraft.techcraftbackend.service.PcCaseService;
import com.techcraft.techcraftbackend.specification.PcCaseSpecification;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PcCaseServiceImpl implements PcCaseService {

    private final PcCaseRepository pcCaseRepository;
    private final CompatibilityChecker compatibilityChecker;
    private final PcCaseMapper pcCaseMapper;

    @Override
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

    @Override
    @Transactional
    public PcCaseResponse updatePcCase(UUID id, UpdatePcCaseRequest request) {
        PcCase pcCase = pcCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + id));

        String trimmedName = request.getName().trim();
        if (pcCaseRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Cấu hình PC với tên '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        pcCase.setName(trimmedName);
        pcCase.setUseCase(StringUtils.hasText(request.getUseCase()) ? request.getUseCase().trim() : null);
        pcCase.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            pcCase.setActive(request.getIsActive());
        }

        if (request.hasItemUpdates()) {
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

            pcCase.setTotalPrice(totalPrice);

            if (pcCase.getItems() == null) {
                pcCase.setItems(new ArrayList<>());
            }

            Map<ProductCategory, PcCaseItem> existingItemsByCategory = pcCase.getItems().stream()
                    .collect(Collectors.toMap(PcCaseItem::getCategory, Function.identity(), (a, b) -> a));

            // Xóa các item thuộc category không còn nằm trong componentsByCategory
            pcCase.getItems().removeIf(item -> !componentsByCategory.containsKey(item.getCategory()));

            // Cập nhật sản phẩm cho category đã tồn tại hoặc thêm mới category chưa có
            for (ProductCategory category : ProductCategory.values()) {
                Product product = componentsByCategory.get(category);
                if (product != null) {
                    PcCaseItem existingItem = existingItemsByCategory.get(category);
                    if (existingItem != null) {
                        existingItem.setProduct(product);
                    } else {
                        PcCaseItem item = PcCaseItem.builder()
                                .category(category)
                                .product(product)
                                .build();
                        pcCase.addItem(item);
                    }
                }
            }
        }

        PcCase updatedPcCase = pcCaseRepository.save(pcCase);
        log.info("Updated PC Case successfully with id: {}, name: {}, totalPrice: {}",
                updatedPcCase.getId(), updatedPcCase.getName(), updatedPcCase.getTotalPrice());

        return pcCaseMapper.toResponse(updatedPcCase);
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityResult validatePcCase(ValidatePcCaseRequest request) {
        return compatibilityChecker.check(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PcCaseResponse> getPcCases(PcCaseFilterRequest filter) {
        if (filter == null) {
            filter = new PcCaseFilterRequest();
        }
        validateFilter(filter);

        Boolean effectiveIsActive = resolveEffectiveIsActive(filter.getIsActive());

        Specification<PcCase> spec = PcCaseSpecification.filter(
                filter.getEffectiveSearch(),
                filter.getUseCase(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                effectiveIsActive
        );

        Pageable pageable = createPageable(filter);
        Page<PcCase> pcCasePage = pcCaseRepository.findAll(spec, pageable);

        return PageResponse.from(pcCasePage, pcCaseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PcCaseResponse getPcCaseById(UUID id) {
        PcCase pcCase = pcCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + id));

        if (!pcCase.isActive() && !isAdmin()) {
            throw new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + id);
        }

        return pcCaseMapper.toResponse(pcCase);
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

    private void validateFilter(PcCaseFilterRequest filter) {
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

    private Pageable createPageable(PcCaseFilterRequest filter) {
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
            case "totalPrice", "total_price", "price" -> "totalPrice";
            case "name" -> "name";
            case "updatedAt", "updated_at" -> "updatedAt";
            default -> throw new BadRequestException(
                    "Trường sắp xếp không hợp lệ: '" + sortBy + "'. Các trường hợp lệ: createdAt, totalPrice, name, updatedAt"
            );
        };
    }
}
