package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.LoginRequest;
import com.techcraft.techcraftbackend.dto.request.RegisterRequest;
import com.techcraft.techcraftbackend.dto.response.LoginResponse;
import com.techcraft.techcraftbackend.dto.response.RefreshTokenResponse;
import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.dto.response.UserSummaryResponse;
import com.techcraft.techcraftbackend.entity.EmailVerificationToken;
import com.techcraft.techcraftbackend.entity.RefreshToken;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.repository.EmailVerificationTokenRepository;
import com.techcraft.techcraftbackend.repository.RefreshTokenRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    @Value("${app.email-verification-token-expiry-hours:24}")
    private long tokenExpiryHours;

    @Value("${jwt.refresh-token-expiry-days:30}")
    private long refreshTokenExpiryDays;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .role(Role.USER)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(savedUser)
                .token(token)
                .expiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS))
                .used(false)
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFullName(), token);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .role(savedUser.getRole())
                .emailVerified(savedUser.isEmailVerified())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Mã xác thực không được để trống.");
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Mã xác thực không tồn tại hoặc không hợp lệ."));

        if (verificationToken.isUsed()) {
            throw new BadRequestException("Mã xác thực đã được sử dụng trước đó.");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Mã xác thực đã hết hạn. Vui lòng yêu cầu gửi lại email xác thực.");
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Email hoặc mật khẩu không chính xác.");
        }

        if (!user.isEmailVerified()) {
            throw new AppException("Tài khoản chưa được xác thực email. Vui lòng kiểm tra email để kích hoạt tài khoản.", HttpStatus.FORBIDDEN);
        }

        String accessToken = jwtUtils.generateAccessToken(user);

        String refreshTokenString = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        setAccessTokenCookie(response, accessToken, jwtUtils.getAccessTokenExpirySeconds());
        setRefreshTokenCookie(response, refreshTokenString, refreshTokenExpiryDays * 24 * 60 * 60);

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenExpirySeconds())
                .user(userSummary)
                .build();
    }

    @Transactional
    public void logout(String refreshTokenString, HttpServletResponse response) {
        if (refreshTokenString != null && !refreshTokenString.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenString).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }

        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(String refreshTokenString, HttpServletResponse response) {
        if (refreshTokenString == null || refreshTokenString.isBlank()) {
            throw new UnauthorizedException("Refresh token không tồn tại.");
        }

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ hoặc không tồn tại."));

        if (tokenEntity.isRevoked()) {
            throw new UnauthorizedException("Refresh token đã bị thu hồi.");
        }

        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = tokenEntity.getUser();

        // Chỉ tạo Access Token mới
        String newAccessToken = jwtUtils.generateAccessToken(user);

        // Chỉ gán cookie accessToken mới
        setAccessTokenCookie(response, newAccessToken, jwtUtils.getAccessTokenExpirySeconds());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenExpirySeconds())
                .build();
    }

    private void setAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
