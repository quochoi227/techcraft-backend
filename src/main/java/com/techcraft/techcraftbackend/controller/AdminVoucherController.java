package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.AdminVoucherFilterRequest;
import com.techcraft.techcraftbackend.dto.request.CreateVoucherRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateVoucherRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherDetailResponse;
import com.techcraft.techcraftbackend.dto.response.VoucherResponse;
import com.techcraft.techcraftbackend.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công!", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getAllVouchers(
            @Valid @ModelAttribute AdminVoucherFilterRequest filter) {
        PageResponse<VoucherResponse> response = voucherService.getAllVouchers(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã giảm giá thành công!", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDetailResponse>> getVoucherDetail(
            @PathVariable UUID id) {
        VoucherDetailResponse response = voucherService.getVoucherDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết mã giảm giá thành công!", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        VoucherResponse response = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã giảm giá thành công!", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(
            @PathVariable UUID id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mã giảm giá thành công!", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VoucherResponse>> toggleVoucherStatus(
            @PathVariable UUID id) {
        VoucherResponse response = voucherService.toggleVoucherStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái mã giảm giá thành công!", response));
    }
}
