package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.ForgotPasswordRequest;
import com.techcraft.techcraftbackend.dto.request.LoginRequest;
import com.techcraft.techcraftbackend.dto.request.RegisterRequest;
import com.techcraft.techcraftbackend.dto.request.ResetPasswordRequest;
import com.techcraft.techcraftbackend.dto.response.LoginResponse;
import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.dto.response.UserSummaryResponse;
import com.techcraft.techcraftbackend.entity.EmailVerificationToken;
import com.techcraft.techcraftbackend.entity.PasswordResetToken;
import com.techcraft.techcraftbackend.entity.RefreshToken;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.UserMapper;
import com.techcraft.techcraftbackend.repository.EmailVerificationTokenRepository;
import com.techcraft.techcraftbackend.repository.PasswordResetTokenRepository;
import com.techcraft.techcraftbackend.repository.RefreshTokenRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.security.JwtUtils;
import com.techcraft.techcraftbackend.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "tokenExpiryHours", 24L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 30L);
        ReflectionTestUtils.setField(authService, "passwordResetTokenExpiryHours", 1L);
    }

    @Test
    void register_Success_UsesUserMapper() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@techcraft.com")
                .password("password123")
                .fullName("New User")
                .phone("0988776655")
                .address("Hà Nội")
                .build();

        UUID generatedId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(generatedId)
                .email("newuser@techcraft.com")
                .passwordHash("encodedPassword")
                .fullName("New User")
                .phone("0988776655")
                .address("Hà Nội")
                .role(Role.USER)
                .emailVerified(false)
                .build();

        RegisterResponse expectedResponse = RegisterResponse.builder()
                .id(generatedId)
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .role(savedUser.getRole())
                .emailVerified(false)
                .build();

        when(userRepository.existsByEmail("newuser@techcraft.com")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class))).thenReturn(null);
        when(userMapper.toRegisterResponse(savedUser)).thenReturn(expectedResponse);

        RegisterResponse actualResponse = authService.register(request);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getEmail(), actualResponse.getEmail());
        assertEquals(expectedResponse.getFullName(), actualResponse.getFullName());

        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("newuser@techcraft.com"), eq("New User"), anyString());
        verify(userMapper).toRegisterResponse(savedUser);
    }

    @Test
    void login_Success_UsesUserMapper() {
        LoginRequest request = LoginRequest.builder()
                .email("test@techcraft.com")
                .password("password123")
                .build();

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@techcraft.com")
                .passwordHash("encodedPassword")
                .fullName("Test User")
                .phone("0987654321")
                .address("TP.HCM")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        UserSummaryResponse expectedUserSummary = UserSummaryResponse.builder()
                .id(userId)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@techcraft.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateAccessToken(user)).thenReturn("mock-access-token");
        when(jwtUtils.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);
        when(userMapper.toUserSummaryResponse(user)).thenReturn(expectedUserSummary);

        LoginResponse response = authService.login(request, httpServletResponse);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals("test@techcraft.com", response.getUser().getEmail());

        verify(userMapper).toUserSummaryResponse(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void forgotPassword_Success_SendsEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@techcraft.com")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@techcraft.com")
                .fullName("Test User")
                .active(true)
                .build();

        PasswordResetToken oldToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("old-token")
                .used(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        List<PasswordResetToken> activeTokens = new ArrayList<>(List.of(oldToken));

        when(userRepository.findByEmail("test@techcraft.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(activeTokens);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(null);

        authService.forgotPassword(request);

        assertTrue(oldToken.isUsed());
        verify(passwordResetTokenRepository).saveAll(activeTokens);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@techcraft.com"), eq("Test User"), anyString());
    }

    @Test
    void forgotPassword_UserNotFound_ThrowsResourceNotFoundException() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@techcraft.com")
                .build();

        when(userRepository.findByEmail("nonexistent@techcraft.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_UserInactive_ThrowsAppException() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("inactive@techcraft.com")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@techcraft.com")
                .fullName("Inactive User")
                .active(false)
                .build();

        when(userRepository.findByEmail("inactive@techcraft.com")).thenReturn(Optional.of(user));

        assertThrows(AppException.class, () -> authService.forgotPassword(request));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_Success_UpdatesPasswordAndRevokesTokens() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-reset-token")
                .newPassword("newPassword123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@techcraft.com")
                .passwordHash("oldHash")
                .emailVerified(false)
                .active(true)
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("valid-reset-token")
                .used(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        RefreshToken activeSession = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("refresh-token")
                .revoked(false)
                .build();

        List<RefreshToken> activeRefreshTokens = new ArrayList<>(List.of(activeSession));

        when(passwordResetTokenRepository.findByToken("valid-reset-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(activeRefreshTokens);

        authService.resetPassword(request);

        assertEquals("newHashedPassword", user.getPasswordHash());
        assertTrue(user.isEmailVerified());
        assertTrue(resetToken.isUsed());
        assertTrue(activeSession.isRevoked());

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(refreshTokenRepository).saveAll(activeRefreshTokens);
    }

    @Test
    void resetPassword_TokenNotFound_ThrowsBadRequestException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("newPassword123")
                .build();

        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_TokenAlreadyUsed_ThrowsBadRequestException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("used-token")
                .newPassword("newPassword123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("used-token")
                .used(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_TokenExpired_ThrowsBadRequestException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("newPassword123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("expired-token")
                .used(false)
                .expiresAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
