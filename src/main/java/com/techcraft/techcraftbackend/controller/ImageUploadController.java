package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.response.CloudinaryUploadResponse;
import com.techcraft.techcraftbackend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/images", "/api/admin/images"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CloudinaryUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        CloudinaryUploadResponse response = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.success("Tải hình ảnh lên Cloudinary thành công!", response));
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<CloudinaryUploadResponse>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) String folder) {
        List<CloudinaryUploadResponse> response = cloudinaryService.uploadImages(files, folder);
        return ResponseEntity.ok(ApiResponse.success("Tải danh sách hình ảnh lên Cloudinary thành công!", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteImageByParam(
            @RequestParam("publicId") String publicId) {
        cloudinaryService.deleteImage(publicId);
        return ResponseEntity.ok(ApiResponse.success("Xoá hình ảnh trên Cloudinary thành công!", null));
    }

    @DeleteMapping("/{*publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteImageByPath(
            @PathVariable String publicId) {
        String cleanPublicId = (publicId != null && publicId.startsWith("/"))
                ? publicId.substring(1)
                : publicId;
        cloudinaryService.deleteImage(cleanPublicId);
        return ResponseEntity.ok(ApiResponse.success("Xoá hình ảnh trên Cloudinary thành công!", null));
    }
}
