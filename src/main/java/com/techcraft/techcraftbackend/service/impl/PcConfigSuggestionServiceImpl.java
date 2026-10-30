package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.mapper.PcCaseMapper;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import com.techcraft.techcraftbackend.service.PcConfigSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PcConfigSuggestionServiceImpl implements PcConfigSuggestionService {

    private final PcCaseRepository pcCaseRepository;
    private final PcCaseMapper pcCaseMapper;

    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @Override
    @Transactional(readOnly = true)
    public String getAvailablePcCasesPromptText() {
        List<PcCase> allCases = pcCaseRepository.findAll();

        List<PcCase> availableCases = allCases.stream()
                .filter(PcCase::isActive)
                .filter(this::isAllComponentsInStock)
                .toList();

        if (availableCases.isEmpty()) {
            return "Hiện tại cửa hàng tạm thời hết hàng linh kiện của tất cả các bộ PC Case sẵn có.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH CÁC CẤU HÌNH PC CASE CÓ SẴN (ĐANG CÒN HÀNG):\n");

        for (PcCase pcCase : availableCases) {
            sb.append("\n----------------------------------------\n");
            sb.append("[PC_CASE_ID: ").append(pcCase.getId()).append("]\n");
            sb.append("Tên cấu hình: ").append(pcCase.getName()).append("\n");
            if (pcCase.getUseCase() != null && !pcCase.getUseCase().isBlank()) {
                sb.append("Mục đích: ").append(pcCase.getUseCase()).append("\n");
            }
            if (pcCase.getTotalPrice() != null) {
                sb.append("Tổng giá: ").append(CURRENCY_FORMAT.format(pcCase.getTotalPrice())).append(" VND\n");
            }
            if (pcCase.getDescription() != null && !pcCase.getDescription().isBlank()) {
                sb.append("Mô tả: ").append(pcCase.getDescription().trim()).append("\n");
            }

            sb.append("Chi tiết linh kiện:\n");
            if (pcCase.getItems() != null) {
                for (PcCaseItem item : pcCase.getItems()) {
                    Product prod = item.getProduct();
                    if (prod != null) {
                        sb.append("  - ").append(item.getCategory()).append(": ")
                                .append(prod.getName());
                        if (prod.getPrice() != null) {
                            sb.append(" (").append(CURRENCY_FORMAT.format(prod.getPrice())).append(" VND)");
                        }
                        String keySpecs = extractKeySpecs(prod.getDetail());
                        if (!keySpecs.isBlank()) {
                            sb.append(" [").append(keySpecs).append("]");
                        }
                        sb.append("\n");
                    }
                }
            }
        }

        return sb.toString();
    }

    @Override
    public Set<UUID> extractPcCaseIds(String text) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return ids;
        }

        Matcher uuidMatcher = UUID_PATTERN.matcher(text);
        while (uuidMatcher.find()) {
            try {
                ids.add(UUID.fromString(uuidMatcher.group()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PcCaseResponse> findFullPcCasesByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<PcCase> pcCases = pcCaseRepository.findAllById(ids);
        return pcCases.stream()
                .filter(PcCase::isActive)
                .map(pcCaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    private boolean isAllComponentsInStock(PcCase pcCase) {
        if (pcCase.getItems() == null || pcCase.getItems().isEmpty()) {
            return false;
        }

        for (PcCaseItem item : pcCase.getItems()) {
            Product product = item.getProduct();
            if (product == null || !product.isActive() || product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
                return false;
            }
        }
        return true;
    }

    private String extractKeySpecs(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return "";
        }

        List<String> specs = new ArrayList<>();
        addIfPresent(detail, "socket", specs);
        addIfPresent(detail, "chipset", specs);
        addIfPresent(detail, "cores", specs, v -> v + " cores");
        addIfPresent(detail, "threads", specs, v -> v + " threads");
        addIfPresent(detail, "vram_gb", specs, v -> v + "GB VRAM");
        addIfPresent(detail, "capacity_gb", specs, v -> v + "GB");
        addIfPresent(detail, "speed_mhz", specs, v -> v + "MHz");
        addIfPresent(detail, "wattage_w", specs, v -> v + "W");
        addIfPresent(detail, "type", specs);

        return String.join(", ", specs);
    }

    private void addIfPresent(Map<String, Object> map, String key, List<String> list) {
        if (map.containsKey(key) && map.get(key) != null) {
            list.add(String.valueOf(map.get(key)));
        }
    }

    private void addIfPresent(Map<String, Object> map, String key, List<String> list, java.util.function.Function<Object, String> formatter) {
        if (map.containsKey(key) && map.get(key) != null) {
            list.add(formatter.apply(map.get(key)));
        }
    }
}
