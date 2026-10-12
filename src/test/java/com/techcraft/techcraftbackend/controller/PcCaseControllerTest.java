package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.PcCaseItemRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
