package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.CloudinaryUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {

    CloudinaryUploadResponse uploadImage(MultipartFile file, String folder);

    List<CloudinaryUploadResponse> uploadImages(List<MultipartFile> files, String folder);

    void deleteImage(String publicId);
}
