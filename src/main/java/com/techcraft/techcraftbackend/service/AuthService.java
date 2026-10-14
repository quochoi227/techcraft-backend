package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.LoginRequest;
import com.techcraft.techcraftbackend.dto.request.RegisterRequest;
import com.techcraft.techcraftbackend.dto.response.LoginResponse;
import com.techcraft.techcraftbackend.dto.response.RefreshTokenResponse;
import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    void verifyEmail(String token);

    LoginResponse login(LoginRequest request, HttpServletResponse response);

    void logout(String refreshTokenString, HttpServletResponse response);

    RefreshTokenResponse refreshToken(String refreshTokenString, HttpServletResponse response);
}
