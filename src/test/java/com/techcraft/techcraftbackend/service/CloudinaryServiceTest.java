package com.techcraft.techcraftbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.techcraft.techcraftbackend.dto.response.CloudinaryUploadResponse;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cloudinaryService, "defaultFolder", "techcraft/products");
    }

    @Test
    void uploadImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "dummy image content".getBytes()
        );

        Map<String, Object> mockUploadResult = new HashMap<>();
        mockUploadResult.put("public_id", "techcraft/products/test-image");
        mockUploadResult.put("url", "http://res.cloudinary.com/demo/image/upload/v1/techcraft/products/test-image.png");
        mockUploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/techcraft/products/test-image.png");
        mockUploadResult.put("format", "png");
        mockUploadResult.put("width", 800);
        mockUploadResult.put("height", 600);
        mockUploadResult.put("bytes", 1024L);
        mockUploadResult.put("created_at", "2026-10-17T12:00:00Z");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(mockUploadResult);

        CloudinaryUploadResponse response = cloudinaryService.uploadImage(file, null);

        assertNotNull(response);
        assertEquals("techcraft/products/test-image", response.getPublicId());
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/techcraft/products/test-image.png", response.getSecureUrl());
        assertEquals("png", response.getFormat());
        assertEquals(800, response.getWidth());
        assertEquals(600, response.getHeight());
        assertEquals(1024L, response.getBytes());
        assertEquals("test-image.png", response.getOriginalFilename());
        verify(uploader).upload(eq(file.getBytes()), argThat(map -> "techcraft/products".equals(map.get("folder"))));
    }

    @Test
    void uploadImage_WithCustomFolder_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "jpeg content".getBytes()
        );

        Map<String, Object> mockUploadResult = new HashMap<>();
        mockUploadResult.put("public_id", "techcraft/users/avatar");
        mockUploadResult.put("url", "http://res.cloudinary.com/demo/image/upload/v1/techcraft/users/avatar.jpg");
        mockUploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/techcraft/users/avatar.jpg");
        mockUploadResult.put("format", "jpg");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(mockUploadResult);

        CloudinaryUploadResponse response = cloudinaryService.uploadImage(file, "techcraft/users");

        assertNotNull(response);
        assertEquals("techcraft/users/avatar", response.getPublicId());
        verify(uploader).upload(eq(file.getBytes()), argThat(map -> "techcraft/users".equals(map.get("folder"))));
    }

    @Test
    void uploadImage_EmptyFile_ThrowsBadRequestException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(emptyFile, null)
        );

        assertTrue(ex.getMessage().contains("Vui lòng chọn tệp hình ảnh để tải lên"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_NullFile_ThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(null, null)
        );

        assertTrue(ex.getMessage().contains("Vui lòng chọn tệp hình ảnh để tải lên"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_InvalidContentType_ThrowsBadRequestException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(pdfFile, null)
        );

        assertTrue(ex.getMessage().contains("không hợp lệ"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_FileSizeExceeded_ThrowsBadRequestException() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                largeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(largeFile, null)
        );

        assertTrue(ex.getMessage().contains("vượt quá giới hạn cho phép"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImage_CloudinaryThrowsIOException_ThrowsBadRequestException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Connection reset"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(file, null)
        );

        assertTrue(ex.getMessage().contains("Lỗi khi tải hình ảnh lên Cloudinary"));
    }

    @Test
    void uploadImages_Success() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", "img1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.webp", "image/webp", "img2".getBytes());

        Map<String, Object> mockResult1 = Map.of(
                "public_id", "p1",
                "secure_url", "https://url1.com",
                "format", "png"
        );
        Map<String, Object> mockResult2 = Map.of(
                "public_id", "p2",
                "secure_url", "https://url2.com",
                "format", "webp"
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(file1.getBytes()), anyMap())).thenReturn(mockResult1);
        when(uploader.upload(eq(file2.getBytes()), anyMap())).thenReturn(mockResult2);

        List<CloudinaryUploadResponse> responses = cloudinaryService.uploadImages(List.of(file1, file2), null);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("p1", responses.get(0).getPublicId());
        assertEquals("p2", responses.get(1).getPublicId());
    }

    @Test
    void uploadImages_EmptyList_ThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImages(Collections.emptyList(), null)
        );

        assertTrue(ex.getMessage().contains("Danh sách tệp tải lên không được để trống"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadImages_ExceedsMaxBatch_ThrowsBadRequestException() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(new MockMultipartFile("files", "img" + i + ".png", "image/png", "content".getBytes()));
        }

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImages(files, null)
        );

        assertTrue(ex.getMessage().contains("tối đa trong một lần là 10"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void deleteImage_Success() throws IOException {
        String publicId = "techcraft/products/sample";
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(Map.of("result", "ok"));

        assertDoesNotThrow(() -> cloudinaryService.deleteImage(publicId));
        verify(uploader).destroy(eq(publicId), anyMap());
    }

    @Test
    void deleteImage_NotFound_ThrowsResourceNotFoundException() throws IOException {
        String publicId = "techcraft/products/non-existent";
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(Map.of("result", "not found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                cloudinaryService.deleteImage(publicId)
        );

        assertTrue(ex.getMessage().contains("Không tìm thấy hình ảnh trên Cloudinary"));
    }

    @Test
    void deleteImage_FailedStatus_ThrowsBadRequestException() throws IOException {
        String publicId = "techcraft/products/failed-del";
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(Map.of("result", "error"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.deleteImage(publicId)
        );

        assertTrue(ex.getMessage().contains("Không thể xoá hình ảnh trên Cloudinary"));
    }

    @Test
    void deleteImage_EmptyPublicId_ThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.deleteImage("   ")
        );

        assertTrue(ex.getMessage().contains("không được để trống"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void deleteImage_CloudinaryThrowsIOException_ThrowsBadRequestException() throws IOException {
        String publicId = "techcraft/products/io-error";
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq(publicId), anyMap())).thenThrow(new IOException("Timeout"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.deleteImage(publicId)
        );

        assertTrue(ex.getMessage().contains("Lỗi khi xoá hình ảnh trên Cloudinary"));
    }
}
