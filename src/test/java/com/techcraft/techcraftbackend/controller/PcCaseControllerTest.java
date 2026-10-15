package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseFilterRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.PcCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PcCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PcCaseService pcCaseService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPcCase_AsAdmin_Returns201Created() throws Exception {
        UUID cpuId = UUID.randomUUID();
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .description("Cấu hình chiến game Full HD")
                .isActive(true)
                .items(List.of(
                        PcCaseItemRequest.builder().category(ProductCategory.CPU).productId(cpuId).build()
                ))
                .build();

        PcCaseResponse response = PcCaseResponse.builder()
                .id(UUID.randomUUID())
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .totalPrice(new BigDecimal("18500000.00"))
                .description("Cấu hình chiến game Full HD")
                .isActive(true)
                .build();

        when(pcCaseService.createPcCase(any(CreatePcCaseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo cấu hình PC mới thành công!"))
                .andExpect(jsonPath("$.data.name").value("Gaming Entry 2025"))
                .andExpect(jsonPath("$.data.use_case").value("gaming"))
                .andExpect(jsonPath("$.data.total_price").value(18500000.00))
                .andExpect(jsonPath("$.data.is_active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPcCase_AsRegularUser_Returns403Forbidden() throws Exception {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .build();

        mockMvc.perform(post("/api/admin/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPcCase_Unauthenticated_Returns401Unauthorized() throws Exception {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .build();

        mockMvc.perform(post("/api/admin/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPcCase_MissingName_Returns400BadRequest() throws Exception {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .useCase("gaming")
                .build();

        mockMvc.perform(post("/api/admin/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").value("Tên cấu hình PC không được để trống"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPcCase_IncompatibleComponents_Returns400WithViolations() throws Exception {
        CreatePcCaseRequest request = CreatePcCaseRequest.builder()
                .name("Gaming Entry 2025")
                .useCase("gaming")
                .items(List.of(
                        PcCaseItemRequest.builder().category(ProductCategory.CPU).productId(UUID.randomUUID()).build()
                ))
                .build();

        CompatibilityResult failedResult = CompatibilityResult.fail(List.of(
                "Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)"
        ));

        when(pcCaseService.createPcCase(any(CreatePcCaseRequest.class)))
                .thenThrow(new BadRequestException(
                        "Cấu hình PC không tương thích: Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)",
                        failedResult
                ));

        mockMvc.perform(post("/api/admin/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.compatible").value(false))
                .andExpect(jsonPath("$.data.violations[0]").value("Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validatePcCase_AsAdmin_Returns200WithCompatibilityResult() throws Exception {
        ValidatePcCaseRequest request = ValidatePcCaseRequest.builder()
                .items(List.of(
                        PcCaseItemRequest.builder().category(ProductCategory.CPU).productId(UUID.randomUUID()).build()
                ))
                .build();

        when(pcCaseService.validatePcCase(any(ValidatePcCaseRequest.class)))
                .thenReturn(CompatibilityResult.ok());

        mockMvc.perform(post("/api/admin/pc-cases/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.compatible").value(true))
                .andExpect(jsonPath("$.data.violations").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePcCase_AsAdmin_Returns200Ok() throws Exception {
        UUID caseId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Gaming Ultra 2026")
                .useCase("gaming")
                .description("Cấu hình nâng cấp chiến game 4K")
                .isActive(true)
                .build();

        PcCaseResponse response = PcCaseResponse.builder()
                .id(caseId)
                .name("Gaming Ultra 2026")
                .useCase("gaming")
                .totalPrice(new BigDecimal("35000000.00"))
                .description("Cấu hình nâng cấp chiến game 4K")
                .isActive(true)
                .build();

        when(pcCaseService.updatePcCase(eq(caseId), any(UpdatePcCaseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/pc-cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật cấu hình PC thành công!"))
                .andExpect(jsonPath("$.data.id").value(caseId.toString()))
                .andExpect(jsonPath("$.data.name").value("Gaming Ultra 2026"))
                .andExpect(jsonPath("$.data.use_case").value("gaming"))
                .andExpect(jsonPath("$.data.total_price").value(35000000.00))
                .andExpect(jsonPath("$.data.is_active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updatePcCase_AsRegularUser_Returns403Forbidden() throws Exception {
        UUID caseId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Gaming Ultra 2026")
                .build();

        mockMvc.perform(put("/api/admin/pc-cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePcCase_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID caseId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Gaming Ultra 2026")
                .build();

        mockMvc.perform(put("/api/admin/pc-cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePcCase_NotFound_Returns404NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Non-existent PC")
                .build();

        when(pcCaseService.updatePcCase(eq(nonExistentId), any(UpdatePcCaseRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + nonExistentId));

        mockMvc.perform(put("/api/admin/pc-cases/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy cấu hình PC với ID: " + nonExistentId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePcCase_MissingName_Returns400BadRequest() throws Exception {
        UUID caseId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .useCase("gaming")
                .build();

        mockMvc.perform(put("/api/admin/pc-cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").value("Tên cấu hình PC không được để trống"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePcCase_IncompatibleComponents_Returns400WithViolations() throws Exception {
        UUID caseId = UUID.randomUUID();
        UpdatePcCaseRequest request = UpdatePcCaseRequest.builder()
                .name("Gaming Incompatible")
                .items(List.of(
                        PcCaseItemRequest.builder().category(ProductCategory.CPU).productId(UUID.randomUUID()).build()
                ))
                .build();

        CompatibilityResult failedResult = CompatibilityResult.fail(List.of(
                "Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)"
        ));

        when(pcCaseService.updatePcCase(eq(caseId), any(UpdatePcCaseRequest.class)))
                .thenThrow(new BadRequestException(
                        "Cấu hình PC không tương thích: Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)",
                        failedResult
                ));

        mockMvc.perform(put("/api/admin/pc-cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.compatible").value(false))
                .andExpect(jsonPath("$.data.violations[0]").value("Socket của CPU (AM5) không tương thích với socket của Mainboard (LGA1700)"));
    }

    @Test
    void getPcCases_Public_Returns200AndPageResponse() throws Exception {
        PcCaseResponse item = PcCaseResponse.builder()
                .id(UUID.randomUUID())
                .name("Gaming Starter")
                .useCase("gaming")
                .totalPrice(new BigDecimal("15000000.00"))
                .isActive(true)
                .build();

        PageResponse<PcCaseResponse> pageResponse = PageResponse.<PcCaseResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(pcCaseService.getPcCases(any(PcCaseFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/pc-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách cấu hình PC thành công!"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Gaming Starter"))
                .andExpect(jsonPath("$.data.content[0].use_case").value("gaming"));
    }

    @Test
    void getPcCases_WithQueryParams_Success() throws Exception {
        PcCaseResponse item = PcCaseResponse.builder()
                .id(UUID.randomUUID())
                .name("Office Basic")
                .useCase("office")
                .totalPrice(new BigDecimal("8000000.00"))
                .isActive(true)
                .build();

        PageResponse<PcCaseResponse> pageResponse = PageResponse.<PcCaseResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(pcCaseService.getPcCases(any(PcCaseFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/pc-cases")
                        .param("search", "office")
                        .param("use_case", "office")
                        .param("min_price", "5000000")
                        .param("max_price", "10000000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort_by", "price")
                        .param("sort_dir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Office Basic"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAdminPcCases_AsAdmin_Returns200() throws Exception {
        PageResponse<PcCaseResponse> emptyResponse = PageResponse.<PcCaseResponse>builder()
                .content(List.of())
                .page(0)
                .size(12)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();

        when(pcCaseService.getPcCases(any(PcCaseFilterRequest.class))).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/admin/pc-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách cấu hình PC thành công!"))
                .andExpect(jsonPath("$.data.total_elements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAdminPcCases_AsRegularUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/pc-cases"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAdminPcCases_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/pc-cases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPcCaseById_Public_Returns200AndPcCaseResponse() throws Exception {
        UUID caseId = UUID.randomUUID();
        PcCaseResponse response = PcCaseResponse.builder()
                .id(caseId)
                .name("Gaming Ultra")
                .useCase("gaming")
                .totalPrice(new BigDecimal("50000000.00"))
                .isActive(true)
                .build();

        when(pcCaseService.getPcCaseById(caseId)).thenReturn(response);

        mockMvc.perform(get("/api/pc-cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết cấu hình PC thành công!"))
                .andExpect(jsonPath("$.data.id").value(caseId.toString()))
                .andExpect(jsonPath("$.data.name").value("Gaming Ultra"));
    }

    @Test
    void getPcCaseById_NotFound_Returns404NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(pcCaseService.getPcCaseById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + nonExistentId));

        mockMvc.perform(get("/api/pc-cases/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy cấu hình PC với ID: " + nonExistentId));
    }
}
