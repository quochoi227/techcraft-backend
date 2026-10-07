package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.LoginRequest;
import com.techcraft.techcraftbackend.dto.request.RegisterRequest;
import com.techcraft.techcraftbackend.dto.response.LoginResponse;
import com.techcraft.techcraftbackend.dto.response.RefreshTokenResponse;
import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công! Vui lòng kiểm tra email để xác thực tài khoản.", response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            HttpServletResponse response) {

        authService.logout(refreshTokenFromCookie, response);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công!", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            HttpServletResponse response) {
        RefreshTokenResponse refreshResponse = authService.refreshToken(refreshTokenFromCookie, response);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công!", refreshResponse));
    }
}
