package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.enums.ProductCategory;

import java.util.List;
import java.util.Map;

public interface CompatibilityChecker {

    CompatibilityResult check(CreatePcCaseRequest request);

    CompatibilityResult check(ValidatePcCaseRequest request);

    CompatibilityResult checkItems(List<PcCaseItemRequest> itemRequests);

    CompatibilityResult checkProducts(Map<ProductCategory, Product> componentsByCategory);

    Map<ProductCategory, Product> resolveProductsByCategory(
            List<PcCaseItemRequest> itemRequests,
            List<String> violations
    );
}
