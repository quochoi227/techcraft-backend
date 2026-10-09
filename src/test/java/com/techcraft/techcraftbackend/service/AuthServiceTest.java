package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.LoginRequest;
import com.techcraft.techcraftbackend.dto.request.RegisterRequest;
import com.techcraft.techcraftbackend.dto.response.LoginResponse;
import com.techcraft.techcraftbackend.dto.response.RegisterResponse;
import com.techcraft.techcraftbackend.dto.response.UserSummaryResponse;
import com.techcraft.techcraftbackend.entity.EmailVerificationToken;
import com.techcraft.techcraftbackend.entity.RefreshToken;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.mapper.UserMapper;
import com.techcraft.techcraftbackend.repository.EmailVerificationTokenRepository;
import com.techcraft.techcraftbackend.repository.RefreshTokenRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "tokenExpiryHours", 24L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 30L);
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
}
