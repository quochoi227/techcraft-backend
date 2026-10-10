package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreateProductRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProductRequest;
import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.techcraft.techcraftbackend.dto.request.ProductFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void getProducts_PublicUser_Returns200AndPageResponse() throws Exception {
        ProductResponse item = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name("Intel Core i5-13400F")
                .price(new BigDecimal("5200000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(productService.getProducts(any(ProductFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách sản phẩm thành công!"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Intel Core i5-13400F"))
                .andExpect(jsonPath("$.data.content[0].stock_quantity").value(15));
    }

    @Test
    void getProducts_WithQueryParams_Success() throws Exception {
        ProductResponse item = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name("MSI GeForce RTX 4060")
                .price(new BigDecimal("8500000.00"))
                .stockQuantity(8)
                .category(ProductCategory.GPU)
                .isActive(true)
                .build();

        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(productService.getProducts(any(ProductFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/products")
                        .param("keyword", "rtx")
                        .param("category", "GPU")
                        .param("min_price", "5000000")
                        .param("max_price", "10000000")
                        .param("in_stock", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort_by", "price")
                        .param("sort_dir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("MSI GeForce RTX 4060"))
                .andExpect(jsonPath("$.data.content[0].category").value("GPU"));
    }

    @Test
    void getProducts_InvalidCategory_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("category", "INVALID_CAT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getProductById_PublicUser_Returns200AndProductResponse() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = ProductResponse.builder()
                .id(id)
                .name("Intel Core i7-13700K")
                .price(new BigDecimal("10500000.00"))
                .stockQuantity(20)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết sản phẩm thành công!"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Intel Core i7-13700K"))
                .andExpect(jsonPath("$.data.category").value("CPU"));
    }

    @Test
    void getProductById_NotFound_Returns404NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductById(id))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện với ID: " + id));
    }

    @Test
    void getProductById_InvalidUUID_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/products/invalid-uuid-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D V2")
                .price(new BigDecimal("11500000.00"))
                .stockQuantity(18)
                .category(ProductCategory.CPU)
                .description("Updated Gaming CPU")
                .detail(Map.of("socket", "AM5", "cores", 8, "threads", 16, "base_clock_ghz", 4.2, "boost_clock_ghz", 5.0, "tdp_w", 120, "memory_type", "DDR5", "integrated_gpu", true))
                .isActive(true)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(id)
                .name(request.getName())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(ProductCategory.CPU)
                .detail(request.getDetail())
                .isActive(true)
                .build();

        when(productService.updateProduct(eq(id), any(UpdateProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin linh kiện thành công!"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("AMD Ryzen 7 7800X3D V2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_WithSnakeCasePayload_Success() throws Exception {
        UUID id = UUID.randomUUID();
        String jsonPayload = """
        {
          "name": "Intel Core i7-13700K Updated",
          "price": 10900000.00,
          "stock_quantity": 25,
          "category": "CPU",
          "description": "Vi xử lý Intel Gen 13 cập nhật",
          "is_active": false,
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
            }
          ]
        }
        """;

        ProductResponse response = ProductResponse.builder()
                .id(id)
                .name("Intel Core i7-13700K Updated")
                .price(new BigDecimal("10900000.00"))
                .stockQuantity(25)
                .category(ProductCategory.CPU)
                .isActive(false)
                .build();

        when(productService.updateProduct(eq(id), any(UpdateProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Intel Core i7-13700K Updated"))
                .andExpect(jsonPath("$.data.stock_quantity").value(25))
                .andExpect(jsonPath("$.data.is_active").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateProduct_AsRegularUser_Returns403Forbidden() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_NotFound_Returns404NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        when(productService.updateProduct(eq(id), any(UpdateProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id));

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện với ID: " + id));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_InvalidBodyMissingName_Returns400BadRequest() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .price(new BigDecimal("11000000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .build();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").value("Tên linh kiện không được để trống"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).deleteProduct(id);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xoá linh kiện thành công!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProduct_AsRegularUser_Returns403Forbidden() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_NotFound_Returns404NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + id))
                .when(productService).deleteProduct(id);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện với ID: " + id));
    }
}

