package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.CreatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.UpdatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.ValidatePcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CompatibilityResult;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.service.PcCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PcCaseController {

    private final PcCaseService pcCaseService;

    @PostMapping({"/admin/pc-cases", "/pc-cases"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PcCaseResponse>> createPcCase(
            @Valid @RequestBody CreatePcCaseRequest request) {
        PcCaseResponse response = pcCaseService.createPcCase(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cấu hình PC mới thành công!", response));
    }

    @PostMapping({"/admin/pc-cases/validate", "/pc-cases/validate"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompatibilityResult>> validatePcCase(
            @Valid @RequestBody ValidatePcCaseRequest request) {
        CompatibilityResult result = pcCaseService.validatePcCase(request);
        String message = result.isCompatible()
                ? "Cấu hình PC tương thích hợp lệ!"
                : "Cấu hình PC có lỗi không tương thích!";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @PutMapping({"/admin/pc-cases/{id}", "/pc-cases/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PcCaseResponse>> updatePcCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePcCaseRequest request) {
        PcCaseResponse response = pcCaseService.updatePcCase(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình PC thành công!", response));
    }
}
