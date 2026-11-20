package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.ProductFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProductRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;

import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    void deleteProduct(UUID id);

    ProductResponse getProductById(UUID id);

    PageResponse<ProductResponse> getProducts(ProductFilterRequest filter);
}
