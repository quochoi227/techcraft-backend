package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.ForgotPasswordRequest;
import com.techcraft.techcraftbackend.dto.request.ResetPasswordRequest;
import com.techcraft.techcraftbackend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Yêu cầu đặt lại mật khẩu thành công (200 OK)")
    void forgotPassword_ValidEmail_Returns200() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("user@techcraft.com")
                .build();

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Yêu cầu đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra email của bạn."));

        verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("Yêu cầu đặt lại mật khẩu thất bại khi email không đúng định dạng (400 Bad Request)")
    void forgotPassword_InvalidEmail_Returns400() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("invalid-email-format")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.data.email").value("Định dạng email không hợp lệ"));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thành công với token và mật khẩu mới hợp lệ (200 OK)")
    void resetPassword_ValidRequest_Returns200() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-reset-token-123")
                .newPassword("NewPassword@123")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới."));

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thất bại khi mật khẩu mới dưới 6 ký tự (400 Bad Request)")
    void resetPassword_ShortPassword_Returns400() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-reset-token-123")
                .newPassword("123")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.data.newPassword").value("Mật khẩu mới phải có ít nhất 6 ký tự"));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu thất bại khi thiếu token (400 Bad Request)")
    void resetPassword_BlankToken_Returns400() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("")
                .newPassword("NewPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.data.token").value("Mã token đặt lại mật khẩu không được để trống"));
    }
}
