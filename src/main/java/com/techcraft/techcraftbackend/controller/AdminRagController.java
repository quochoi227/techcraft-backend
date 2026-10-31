package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.response.RagDocumentResponse;
import com.techcraft.techcraftbackend.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/rag/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRagController {

    private final RagService ragService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RagDocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        RagDocumentResponse response = ragService.uploadAndIndexDocument(file, title);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải lên và lập chỉ mục tài liệu RAG thành công!", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RagDocumentResponse>>> getAllDocuments() {
        List<RagDocumentResponse> response = ragService.getAllDocuments();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài liệu RAG thành công!", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        ragService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài liệu RAG thành công!", null));
    }
}
