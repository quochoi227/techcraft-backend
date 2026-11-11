package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateVoucherRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherDetailResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.enums.DiscountType;
import com.techcraft.techcraftbackend.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoucherService voucherService;

    @Test
    @DisplayName("Admin tạo voucher thành công (201 Created)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void createVoucher_AsAdmin_Returns201Created() throws Exception {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("NEWYEAR2026")
                .name("Tết 2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(15))
                .maxDiscountAmount(BigDecimal.valueOf(300000))
                .minOrderAmount(BigDecimal.valueOf(1000000))
                .startDate(Instant.now())
                .endDate(Instant.now().plus(15, ChronoUnit.DAYS))
                .usageLimit(100)
                .userUsageLimit(1)
                .isActive(true)
                .build();

        VoucherResponse response = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("NEWYEAR2026")
                .name("Tết 2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(15))
                .valid(true)
                .build();

        when(voucherService.createVoucher(any(CreateVoucherRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("NEWYEAR2026"));
    }

    @Test
    @DisplayName("User thường không có quyền tạo voucher (403 Forbidden)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void createVoucher_AsUser_Returns403Forbidden() throws Exception {
        CreateVoucherRequest request = CreateVoucherRequest.builder()
                .code("TEST403")
                .name("Test")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000))
                .startDate(Instant.now())
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        mockMvc.perform(post("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin lấy danh sách tất cả voucher thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getAllVouchers_AsAdmin_Returns200Ok() throws Exception {
        VoucherResponse voucher = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("ADMINLIST")
                .name("Danh sách admin")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .build();

        PageResponse<VoucherResponse> pageResponse = PageResponse.<VoucherResponse>builder()
                .content(List.of(voucher))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(voucherService.getAllVouchers(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].code").value("ADMINLIST"));
    }

    @Test
    @DisplayName("Admin xem chi tiết voucher kèm thống kê sử dụng thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getVoucherDetail_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        VoucherResponse voucher = VoucherResponse.builder()
                .id(id)
                .code("DETAIL123")
                .name("Chi tiết")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .build();

        VoucherDetailResponse detailResponse = VoucherDetailResponse.builder()
                .voucher(voucher)
                .totalDiscountGiven(BigDecimal.valueOf(250000))
                .totalOrdersCount(5)
                .recentUsages(Collections.emptyList())
                .build();

        when(voucherService.getVoucherDetail(id)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/admin/vouchers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrdersCount").value(5))
                .andExpect(jsonPath("$.data.totalDiscountGiven").value(250000));
    }

    @Test
    @DisplayName("Admin cập nhật voucher thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void updateVoucher_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateVoucherRequest request = UpdateVoucherRequest.builder()
                .name("Tên đã cập nhật")
                .usageLimit(500)
                .build();

        VoucherResponse response = VoucherResponse.builder()
                .id(id)
                .code("UPDATE123")
                .name("Tên đã cập nhật")
                .usageLimit(500)
                .build();

        when(voucherService.updateVoucher(eq(id), any(UpdateVoucherRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/vouchers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tên đã cập nhật"));
    }

    @Test
    @DisplayName("Admin chuyển đổi trạng thái voucher thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void toggleVoucherStatus_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        VoucherResponse response = VoucherResponse.builder()
                .id(id)
                .code("TOGGLE123")
                .active(false)
                .build();

        when(voucherService.toggleVoucherStatus(id)).thenReturn(response);

        mockMvc.perform(patch("/api/admin/vouchers/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("Admin xóa voucher thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void deleteVoucher_AsAdmin_Returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(voucherService).deleteVoucher(id);

        mockMvc.perform(delete("/api/admin/vouchers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
