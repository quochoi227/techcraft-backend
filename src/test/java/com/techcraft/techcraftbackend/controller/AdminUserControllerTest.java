package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.AdminUpdateUserRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateUserStatusRequest;
import com.techcraft.techcraftbackend.dto.request.UserFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.UserResponse;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Admin lấy danh sách người dùng thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getUsers_AsAdmin_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("user@techcraft.com")
                .fullName("User One")
                .role(Role.USER)
                .active(true)
                .build();

        PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder()
                .content(List.of(userResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(userService.getUsers(any(UserFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("user@techcraft.com"));
    }

    @Test
    @DisplayName("User thường không thể truy cập danh sách quản lý user của Admin (403 Forbidden)")
    @WithMockUser(username = "user@techcraft.com", roles = "USER")
    void getUsers_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin xem chi tiết user theo ID thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getUserById_AsAdmin_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("user@techcraft.com")
                .fullName("User One")
                .role(Role.USER)
                .active(true)
                .build();

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("user@techcraft.com"));
    }

    @Test
    @DisplayName("Admin cập nhật thông tin user thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void adminUpdateUser_AsAdmin_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .fullName("Updated Name")
                .role(Role.ADMIN)
                .active(true)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("user@techcraft.com")
                .fullName("Updated Name")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userService.adminUpdateUser(eq("admin@techcraft.com"), eq(userId), any(AdminUpdateUserRequest.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái user qua PATCH thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void updateUserStatus_AsAdmin_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .active(false)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("user@techcraft.com")
                .active(false)
                .build();

        when(userService.updateUserStatus(eq("admin@techcraft.com"), eq(userId), any(UpdateUserStatusRequest.class)))
                .thenReturn(userResponse);

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("User thường không có quyền cập nhật trạng thái user (403 Forbidden)")
    @WithMockUser(username = "user@techcraft.com", roles = "USER")
    void updateUserStatus_AsUser_Returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .active(false)
                .build();

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
