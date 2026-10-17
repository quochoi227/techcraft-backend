package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.response.CloudinaryUploadResponse;
import com.techcraft.techcraftbackend.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImage_AsAdmin_Returns200Ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-product.png",
                "image/png",
                "image bytes".getBytes()
        );

        CloudinaryUploadResponse response = CloudinaryUploadResponse.builder()
                .publicId("techcraft/products/test-product")
                .secureUrl("https://res.cloudinary.com/demo/image/upload/v1/techcraft/products/test-product.png")
                .format("png")
                .build();

        when(cloudinaryService.uploadImage(any(), eq("techcraft/products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("folder", "techcraft/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải hình ảnh lên Cloudinary thành công!"))
                .andExpect(jsonPath("$.data.publicId").value("techcraft/products/test-product"))
                .andExpect(jsonPath("$.data.secureUrl").value("https://res.cloudinary.com/demo/image/upload/v1/techcraft/products/test-product.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImage_ViaAdminPath_AsAdmin_Returns200Ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin-product.png",
                "image/png",
                "image bytes".getBytes()
        );

        CloudinaryUploadResponse response = CloudinaryUploadResponse.builder()
                .publicId("techcraft/products/admin-product")
                .secureUrl("https://res.cloudinary.com/demo/image/upload/v1/techcraft/products/admin-product.png")
                .build();

        when(cloudinaryService.uploadImage(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/images/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("techcraft/products/admin-product"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadImage_AsRegularUser_Returns403Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "image bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/images/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_Unauthenticated_Returns401Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "image bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/images/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImages_AsAdmin_Returns200Ok() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", "bytes1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "bytes2".getBytes());

        List<CloudinaryUploadResponse> responseList = List.of(
                CloudinaryUploadResponse.builder().publicId("p1").secureUrl("https://url1").build(),
                CloudinaryUploadResponse.builder().publicId("p2").secureUrl("https://url2").build()
        );

        when(cloudinaryService.uploadImages(any(), any())).thenReturn(responseList);

        mockMvc.perform(multipart("/api/images/upload-multiple")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải danh sách hình ảnh lên Cloudinary thành công!"))
                .andExpect(jsonPath("$.data[0].publicId").value("p1"))
                .andExpect(jsonPath("$.data[1].publicId").value("p2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteImage_ByRequestParam_AsAdmin_Returns200Ok() throws Exception {
        String publicId = "techcraft/products/sample-id";
        doNothing().when(cloudinaryService).deleteImage(publicId);

        mockMvc.perform(delete("/api/images").param("publicId", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xoá hình ảnh trên Cloudinary thành công!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteImage_ByPathVariable_AsAdmin_Returns200Ok() throws Exception {
        String publicId = "techcraft/products/sample-path-id";
        doNothing().when(cloudinaryService).deleteImage(publicId);

        mockMvc.perform(delete("/api/images/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xoá hình ảnh trên Cloudinary thành công!"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteImage_AsRegularUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/images").param("publicId", "techcraft/products/sample"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteImage_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/images").param("publicId", "techcraft/products/sample"))
                .andExpect(status().isUnauthorized());
    }
}
