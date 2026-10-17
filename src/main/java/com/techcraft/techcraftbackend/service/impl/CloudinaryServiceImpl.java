package com.techcraft.techcraftbackend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techcraft.techcraftbackend.dto.response.CloudinaryUploadResponse;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_BATCH_FILES = 10;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/svg+xml"
    );

    private final Cloudinary cloudinary;

    @Value("${cloudinary.default-folder:techcraft/products}")
    private String defaultFolder;

    @Override
    public CloudinaryUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        String targetFolder = StringUtils.hasText(folder) ? folder.trim() : defaultFolder;

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "image"
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            CloudinaryUploadResponse response = toUploadResponse(uploadResult, file.getOriginalFilename());
            log.info("Uploaded image to Cloudinary successfully. PublicId: {}, URL: {}",
                    response.getPublicId(), response.getSecureUrl());
            return response;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi tải hình ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public List<CloudinaryUploadResponse> uploadImages(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Danh sách tệp tải lên không được để trống");
        }

        if (files.size() > MAX_BATCH_FILES) {
            throw new BadRequestException("Số lượng tệp tải lên tối đa trong một lần là " + MAX_BATCH_FILES + " hình ảnh");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }

        String targetFolder = StringUtils.hasText(folder) ? folder.trim() : defaultFolder;
        List<CloudinaryUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            responses.add(uploadImage(file, targetFolder));
        }

        return responses;
    }

    @Override
    public void deleteImage(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new BadRequestException("Mã định danh hình ảnh (publicId) không được để trống");
        }

        String trimmedPublicId = publicId.trim();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(trimmedPublicId, ObjectUtils.emptyMap());

            String status = (String) result.get("result");
            if ("not found".equalsIgnoreCase(status)) {
                throw new ResourceNotFoundException("Không tìm thấy hình ảnh trên Cloudinary với publicId: " + trimmedPublicId);
            }
            if (!"ok".equalsIgnoreCase(status)) {
                throw new BadRequestException("Không thể xoá hình ảnh trên Cloudinary. Phản hồi: " + status);
            }

            log.info("Deleted image from Cloudinary successfully. PublicId: {}", trimmedPublicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi xoá hình ảnh trên Cloudinary: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp hình ảnh để tải lên");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Kích thước tệp '" + file.getOriginalFilename() +
                    "' vượt quá giới hạn cho phép (tối đa 10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Định dạng tệp '" + file.getOriginalFilename() +
                    "' không hợp lệ. Chỉ chấp nhận các định dạng ảnh: JPG, JPEG, PNG, WEBP, GIF, BMP, SVG");
        }
    }

    private CloudinaryUploadResponse toUploadResponse(Map<String, Object> uploadResult, String originalFilename) {
        return CloudinaryUploadResponse.builder()
                .publicId((String) uploadResult.get("public_id"))
                .url((String) uploadResult.get("url"))
                .secureUrl((String) uploadResult.get("secure_url"))
                .format((String) uploadResult.get("format"))
                .width(uploadResult.get("width") != null ? ((Number) uploadResult.get("width")).intValue() : null)
                .height(uploadResult.get("height") != null ? ((Number) uploadResult.get("height")).intValue() : null)
                .bytes(uploadResult.get("bytes") != null ? ((Number) uploadResult.get("bytes")).longValue() : null)
                .originalFilename(originalFilename)
                .createdAt(uploadResult.get("created_at") != null ? uploadResult.get("created_at").toString() : null)
                .build();
    }
}
