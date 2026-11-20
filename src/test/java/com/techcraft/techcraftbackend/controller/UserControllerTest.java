package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.UpdateProfileRequest;
import com.techcraft.techcraftbackend.dto.response.UserResponse;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Lấy thông tin cá nhân thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "customer@techcraft.com", roles = "USER")
    void getProfile_AsAuthenticatedUser_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("customer@techcraft.com")
                .fullName("Customer Name")
                .phone("0987654321")
                .address("123 Street")
                .role(Role.USER)
                .emailVerified(true)
                .active(true)
                .build();

        when(userService.getProfile("customer@techcraft.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer@techcraft.com"))
                .andExpect(jsonPath("$.data.fullName").value("Customer Name"))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer@techcraft.com"));
    }

    @Test
    @DisplayName("Lấy thông tin cá nhân thất bại khi chưa đăng nhập (401 Unauthorized)")
    void getProfile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cập nhật thông tin cá nhân thành công khi dữ liệu hợp lệ (200 OK)")
    @WithMockUser(username = "customer@techcraft.com", roles = "USER")
    void updateProfile_AsAuthenticatedUser_Returns200() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("New Name")
                .email("customer_new@techcraft.com")
                .phone("0900000000")
                .address("456 New Street")
                .build();

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .email("customer_new@techcraft.com")
                .fullName("New Name")
                .phone("0900000000")
                .address("456 New Street")
                .role(Role.USER)
                .emailVerified(true)
                .active(true)
                .build();

        when(userService.updateProfile(eq("customer@techcraft.com"), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer_new@techcraft.com"))
                .andExpect(jsonPath("$.data.fullName").value("New Name"));
    }

    @Test
    @DisplayName("Cập nhật thông tin cá nhân thất bại khi thiếu họ tên hoặc email không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "customer@techcraft.com", roles = "USER")
    void updateProfile_InvalidData_Returns400() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("")
                .email("invalid-email")
                .build();

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cập nhật thông tin cá nhân thất bại khi email đã trùng (409 Conflict)")
    @WithMockUser(username = "customer@techcraft.com", roles = "USER")
    void updateProfile_DuplicateEmail_Returns409() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Valid Name")
                .email("existing@techcraft.com")
                .build();

        when(userService.updateProfile(eq("customer@techcraft.com"), any(UpdateProfileRequest.class)))
                .thenThrow(new DuplicateResourceException("Email đã được sử dụng bởi người dùng khác. Vui lòng chọn email khác."));

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
