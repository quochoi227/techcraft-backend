package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.ValidateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.VoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.ValidateVoucherResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getPublicVouchers(
            @Valid @ModelAttribute VoucherFilterRequest filter) {
        PageResponse<VoucherResponse> response = voucherService.getPublicVouchers(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá thành công!", response));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAvailableVouchers() {
        List<VoucherResponse> response = voucherService.getAvailableVouchers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá khả dụng thành công!", response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherByCode(@PathVariable String code) {
        VoucherResponse response = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin mã giảm giá thành công!", response));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateVoucherResponse>> validateVoucher(
            Principal principal,
            @Valid @RequestBody ValidateVoucherRequest request) {
        String userEmail = principal != null ? principal.getName() : null;
        ValidateVoucherResponse response = voucherService.validateVoucher(
                request.getCode(),
                request.getOrderAmount(),
                userEmail
        );
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
