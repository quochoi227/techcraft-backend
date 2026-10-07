package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_AsAdmin_Returns201Created() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .description("Gaming CPU")
                .detail(Map.of("socket", "AM5", "cores", 8, "threads", 16, "base_clock_ghz", 4.2, "boost_clock_ghz", 5.0, "tdp_w", 120, "memory_type", "DDR5", "integrated_gpu", true))
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(ProductCategory.CPU)
                .detail(request.getDetail())
                .isActive(true)
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo linh kiện máy tính mới thành công!"))
                .andExpect(jsonPath("$.data.name").value("AMD Ryzen 7 7800X3D"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_AsRegularUser_Returns403Forbidden() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_Unauthenticated_Returns401Unauthorized() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_InvalidBodyMissingName_Returns400BadRequest() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").value("Tên linh kiện không được để trống"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_WithSnakeCasePayload_Success() throws Exception {
        String jsonPayload = """
        {
          "name": "Intel Core i7-13700K",
          "price": 10500000.00,
          "stock_quantity": 20,
          "category": "CPU",
          "description": "Vi xử lý Intel Gen 13 mạnh mẽ cho gaming và đồ họa",
          "is_active": true,
          "detail": {
            "socket": "LGA1700",
            "cores": 16,
            "threads": 24,
            "base_clock_ghz": 3.4,
            "boost_clock_ghz": 5.4,
            "tdp_w": 125,
            "memory_type": "DDR4/DDR5",
            "integrated_gpu": true
          },
          "images": [
            {
              "image_url": "https://res.cloudinary.com/techcraft/cpu-i7-primary.png",
              "is_primary": true,
              "display_order": 0
            },
            {
              "image_url": "https://res.cloudinary.com/techcraft/cpu-i7-box.png",
              "is_primary": false,
              "display_order": 1
            }
          ]
        }
        """;

        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(20)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Intel Core i7-13700K"))
                .andExpect(jsonPath("$.data.stock_quantity").value(20));
    }
}
