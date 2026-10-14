package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.detail.*;
import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.service.CompatibilityChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityCheckerServiceImpl implements CompatibilityChecker {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CompatibilityResult check(CreatePcCaseRequest request) {
        if (request == null) {
            return CompatibilityResult.fail(List.of("Yêu cầu kiểm tra cấu hình PC không được để trống"));
        }
        return checkItems(request.resolveItems());
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityResult check(UpdatePcCaseRequest request) {
        if (request == null) {
            return CompatibilityResult.fail(List.of("Yêu cầu kiểm tra cấu hình PC không được để trống"));
        }
        return checkItems(request.resolveItems());
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityResult check(ValidatePcCaseRequest request) {
        if (request == null) {
            return CompatibilityResult.fail(List.of("Yêu cầu kiểm tra cấu hình PC không được để trống"));
        }
        return checkItems(request.resolveItems());
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityResult checkItems(List<PcCaseItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return CompatibilityResult.fail(List.of("Danh sách linh kiện của cấu hình PC không được để trống"));
        }

        List<String> violations = new ArrayList<>();
        Map<ProductCategory, Product> componentsByCategory = resolveProductsByCategory(itemRequests, violations);

        CompatibilityResult productResult = checkProducts(componentsByCategory);
        violations.addAll(productResult.getViolations());

        return violations.isEmpty()
                ? CompatibilityResult.ok()
                : CompatibilityResult.fail(violations);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ProductCategory, Product> resolveProductsByCategory(
            List<PcCaseItemRequest> itemRequests,
            List<String> violations
    ) {
        List<UUID> productIds = itemRequests.stream()
                .map(PcCaseItemRequest::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<ProductCategory, Product> componentsByCategory = new EnumMap<>(ProductCategory.class);

        for (PcCaseItemRequest itemReq : itemRequests) {
            UUID productId = itemReq.getProductId();
            if (productId == null) {
                violations.add("ID linh kiện (product_id) không được để trống");
                continue;
            }

            Product product = productsById.get(productId);
            if (product == null) {
                violations.add("Không tìm thấy linh kiện với ID: " + productId);
                continue;
            }

            if (itemReq.getCategory() != null && itemReq.getCategory() != product.getCategory()) {
                violations.add(String.format(
                        "Linh kiện '%s' thuộc danh mục %s, không khớp với danh mục được chọn (%s)",
                        product.getName(), product.getCategory(), itemReq.getCategory()
                ));
                continue;
            }

            ProductCategory category = product.getCategory();
            if (componentsByCategory.containsKey(category)) {
                violations.add(String.format(
                        "Danh mục %s bị trùng lặp (mỗi danh mục chỉ được chọn 1 linh kiện)",
                        category
                ));
                continue;
            }

            componentsByCategory.put(category, product);
        }

        return componentsByCategory;
    }

    @Override
    public CompatibilityResult checkProducts(Map<ProductCategory, Product> componentsByCategory) {
        List<String> violations = new ArrayList<>();

        if (componentsByCategory == null || componentsByCategory.isEmpty()) {
            return CompatibilityResult.fail(List.of("Danh sách linh kiện của cấu hình PC không được để trống"));
        }

        // Kiểm tra đủ các danh mục bắt buộc
        for (ProductCategory category : ProductCategory.values()) {
            if (!componentsByCategory.containsKey(category)) {
                violations.add("Thiếu linh kiện cho danh mục bắt buộc: " + category);
            }
        }

        // Ràng buộc 9: Tất cả linh kiện phải đang hoạt động và có stock_quantity > 0
        for (Map.Entry<ProductCategory, Product> entry : componentsByCategory.entrySet()) {
            Product product = entry.getValue();
            if (!product.isActive()) {
                violations.add(String.format(
                        "Linh kiện '%s' (%s) hiện đã ngừng kinh doanh",
                        product.getName(), product.getCategory()
                ));
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
                violations.add(String.format(
                        "Linh kiện '%s' (%s) đã hết hàng trong kho (stock_quantity = %d)",
                        product.getName(), product.getCategory(),
                        product.getStockQuantity() != null ? product.getStockQuantity() : 0
                ));
            }
        }

        CpuDetail cpu = parseDetail(componentsByCategory.get(ProductCategory.CPU), CpuDetail.class, violations);
        MainboardDetail mainboard = parseDetail(componentsByCategory.get(ProductCategory.MAINBOARD), MainboardDetail.class, violations);
        RamDetail ram = parseDetail(componentsByCategory.get(ProductCategory.RAM), RamDetail.class, violations);
        GpuDetail gpu = parseDetail(componentsByCategory.get(ProductCategory.GPU), GpuDetail.class, violations);
        PsuDetail psu = parseDetail(componentsByCategory.get(ProductCategory.PSU), PsuDetail.class, violations);
        CaseDetail pcCase = parseDetail(componentsByCategory.get(ProductCategory.CASE), CaseDetail.class, violations);
        CpuCoolerDetail cooler = parseDetail(componentsByCategory.get(ProductCategory.CPU_COOLER), CpuCoolerDetail.class, violations);

        // Ràng buộc 1: CPU.socket == MAINBOARD.socket
        if (cpu != null && mainboard != null && cpu.getSocket() != null && mainboard.getSocket() != null) {
            if (!cpu.getSocket().trim().equalsIgnoreCase(mainboard.getSocket().trim())) {
                violations.add(String.format(
                        "Socket của CPU (%s) không tương thích với socket của Mainboard (%s)",
                        cpu.getSocket().trim(), mainboard.getSocket().trim()
                ));
            }
        }

        // Ràng buộc 2: CPU.memory_type chứa MAINBOARD.memory_type (hoặc ngược lại)
        if (cpu != null && mainboard != null && cpu.getMemoryType() != null && mainboard.getMemoryType() != null) {
            String cpuMemory = cpu.getMemoryType().trim().toUpperCase(Locale.ROOT);
            String mbMemory = mainboard.getMemoryType().trim().toUpperCase(Locale.ROOT);
            if (!(cpuMemory.contains(mbMemory) || mbMemory.contains(cpuMemory))) {
                violations.add(String.format(
                        "Loại RAM hỗ trợ của CPU (%s) không tương thích với loại RAM của Mainboard (%s)",
                        cpu.getMemoryType().trim(), mainboard.getMemoryType().trim()
                ));
            }
        }

        // Ràng buộc 3: RAM.type == MAINBOARD.memory_type
        if (ram != null && mainboard != null && ram.getType() != null && mainboard.getMemoryType() != null) {
            if (!ram.getType().trim().equalsIgnoreCase(mainboard.getMemoryType().trim())) {
                violations.add(String.format(
                        "Loại RAM (%s) không tương thích với loại RAM hỗ trợ của Mainboard (%s)",
                        ram.getType().trim(), mainboard.getMemoryType().trim()
                ));
            }
        }

        // Ràng buộc 4: MAINBOARD.form_factor nằm trong CASE.form_factor_support
        if (mainboard != null && pcCase != null && mainboard.getFormFactor() != null && pcCase.getFormFactorSupport() != null) {
            String mbFormFactor = mainboard.getFormFactor().trim();
            boolean formFactorSupported = pcCase.getFormFactorSupport().stream()
                    .anyMatch(ff -> ff != null && ff.trim().equalsIgnoreCase(mbFormFactor));
            if (!formFactorSupported) {
                violations.add(String.format(
                        "Kích thước Mainboard (%s) không nằm trong danh sách hỗ trợ của vỏ Case (%s)",
                        mbFormFactor, String.join(", ", pcCase.getFormFactorSupport())
                ));
            }
        }

        // Ràng buộc 5: GPU.length_mm <= CASE.max_gpu_length_mm
        if (gpu != null && pcCase != null && gpu.getLengthMm() != null && pcCase.getMaxGpuLengthMm() != null) {
            if (gpu.getLengthMm() > pcCase.getMaxGpuLengthMm()) {
                violations.add(String.format(
                        "Chiều dài GPU (%dmm) vượt quá chiều dài GPU tối đa của vỏ Case (%dmm)",
                        gpu.getLengthMm(), pcCase.getMaxGpuLengthMm()
                ));
            }
        }

        // Ràng buộc 6: CPU_COOLER.height_mm <= CASE.max_cooler_height_mm
        if (cooler != null && pcCase != null && cooler.getHeightMm() != null && pcCase.getMaxCoolerHeightMm() != null) {
            if (cooler.getHeightMm() > pcCase.getMaxCoolerHeightMm()) {
                violations.add(String.format(
                        "Chiều cao tản nhiệt CPU (%dmm) vượt quá chiều cao tản nhiệt tối đa của vỏ Case (%dmm)",
                        cooler.getHeightMm(), pcCase.getMaxCoolerHeightMm()
                ));
            }
        }

        // Ràng buộc 7: CPU_COOLER.socket_support chứa CPU.socket
        if (cooler != null && cpu != null && cooler.getSocketSupport() != null && cpu.getSocket() != null) {
            String cpuSocket = cpu.getSocket().trim();
            boolean socketSupported = cooler.getSocketSupport().stream()
                    .anyMatch(s -> s != null && s.trim().equalsIgnoreCase(cpuSocket));
            if (!socketSupported) {
                violations.add(String.format(
                        "Tản nhiệt CPU không hỗ trợ socket của CPU (%s). Các socket hỗ trợ: %s",
                        cpuSocket, String.join(", ", cooler.getSocketSupport())
                ));
            }
        }

        // Ràng buộc 8: Tổng TDP (CPU + GPU) <= PSU.wattage_w * 0.8 (hệ số an toàn)
        if (cpu != null && psu != null && cpu.getTdpW() != null && psu.getWattageW() != null) {
            int cpuTdp = cpu.getTdpW();
            int gpuTdp = (gpu != null && gpu.getTdpW() != null) ? gpu.getTdpW() : 0;
            int totalTdp = cpuTdp + gpuTdp;
            double maxSafeWattage = psu.getWattageW() * 0.8;
            if (totalTdp > maxSafeWattage) {
                violations.add(String.format(
                        Locale.US,
                        "Tổng TDP của CPU và GPU (%dW) vượt quá công suất an toàn 80%% của nguồn PSU (%.0fW trên tổng %dW)",
                        totalTdp, maxSafeWattage, psu.getWattageW()
                ));
            }
        }

        return violations.isEmpty()
                ? CompatibilityResult.ok()
                : CompatibilityResult.fail(violations);
    }

    private <T> T parseDetail(Product product, Class<T> clazz, List<String> violations) {
        if (product == null || product.getDetail() == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(product.getDetail(), clazz);
        } catch (IllegalArgumentException ex) {
            log.error("Failed to parse detail for product {} ({}): {}", product.getId(), product.getName(), ex.getMessage());
            violations.add(String.format("Thông số kỹ thuật của linh kiện '%s' (%s) không hợp lệ", product.getName(), product.getCategory()));
            return null;
        }
    }
}
