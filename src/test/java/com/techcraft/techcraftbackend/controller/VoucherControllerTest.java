package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.ValidateVoucherRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ValidateVoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.enums.DiscountType;
import com.techcraft.techcraftbackend.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoucherService voucherService;

    @Test
    @DisplayName("Lấy danh sách voucher công khai thành công (200 OK)")
    void getPublicVouchers_Returns200Ok() throws Exception {
        VoucherResponse voucher = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("SALE2026")
                .name("Giảm giá 2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .valid(true)
                .build();

        PageResponse<VoucherResponse> pageResponse = PageResponse.<VoucherResponse>builder()
                .content(List.of(voucher))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(voucherService.getPublicVouchers(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].code").value("SALE2026"));
    }

    @Test
    @DisplayName("Lấy danh sách voucher khả dụng thành công (200 OK)")
    void getAvailableVouchers_Returns200Ok() throws Exception {
        VoucherResponse voucher = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("FREESHIP")
                .name("Hỗ trợ vận chuyển")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(30000))
                .valid(true)
                .build();

        when(voucherService.getAvailableVouchers()).thenReturn(List.of(voucher));

        mockMvc.perform(get("/api/vouchers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("FREESHIP"));
    }

    @Test
    @DisplayName("Lấy chi tiết voucher theo mã code thành công (200 OK)")
    void getVoucherByCode_Returns200Ok() throws Exception {
        VoucherResponse voucher = VoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("TECH100K")
                .name("Giảm 100K")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(100000))
                .valid(true)
                .build();

        when(voucherService.getVoucherByCode("TECH100K")).thenReturn(voucher);

        mockMvc.perform(get("/api/vouchers/TECH100K"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TECH100K"));
    }

    @Test
    @DisplayName("Validate voucher thành công (200 OK)")
    void validateVoucher_Success_Returns200Ok() throws Exception {
        ValidateVoucherRequest request = ValidateVoucherRequest.builder()
                .code("TECH100K")
                .orderAmount(BigDecimal.valueOf(1500000))
                .build();

        ValidateVoucherResponse response = ValidateVoucherResponse.builder()
                .valid(true)
                .code("TECH100K")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(100000))
                .discountAmount(BigDecimal.valueOf(100000))
                .originalAmount(BigDecimal.valueOf(1500000))
                .finalAmount(BigDecimal.valueOf(1400000))
                .message("Áp dụng mã giảm giá thành công!")
                .build();

        when(voucherService.validateVoucher(eq("TECH100K"), eq(BigDecimal.valueOf(1500000)), any())).thenReturn(response);

        mockMvc.perform(post("/api/vouchers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(100000))
                .andExpect(jsonPath("$.data.finalAmount").value(1400000));
    }
}
