package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AdminUpdateUserRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateProfileRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateUserStatusRequest;
import com.techcraft.techcraftbackend.dto.request.UserFilterRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.UserResponse;
import com.techcraft.techcraftbackend.entity.RefreshToken;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.UserMapper;
import com.techcraft.techcraftbackend.repository.RefreshTokenRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private User sampleAdmin;
    private UserResponse sampleUserResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@techcraft.com")
                .fullName("Nguyễn Văn User")
                .phone("0987654321")
                .address("123 Phố Huế, Hà Nội")
                .role(Role.USER)
                .emailVerified(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@techcraft.com")
                .fullName("Trần Admin")
                .phone("0912345678")
                .address("456 Lê Lợi, TP.HCM")
                .role(Role.ADMIN)
                .emailVerified(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleUserResponse = UserResponse.builder()
                .id(sampleUser.getId())
                .email(sampleUser.getEmail())
                .fullName(sampleUser.getFullName())
                .phone(sampleUser.getPhone())
                .address(sampleUser.getAddress())
                .role(sampleUser.getRole())
                .emailVerified(sampleUser.isEmailVerified())
                .active(sampleUser.isActive())
                .createdAt(sampleUser.getCreatedAt())
                .updatedAt(sampleUser.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Lấy thông tin profile thành công khi email tồn tại")
    void getProfile_Success() {
        when(userRepository.findByEmail("user@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse result = userService.getProfile("user@techcraft.com");

        assertNotNull(result);
        assertEquals(sampleUser.getId(), result.getId());
        assertEquals("user@techcraft.com", result.getEmail());
        verify(userRepository).findByEmail("user@techcraft.com");
    }

    @Test
    @DisplayName("Lấy profile ném ngoại lệ khi không tìm thấy user")
    void getProfile_NotFound() {
        when(userRepository.findByEmail("unknown@techcraft.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile("unknown@techcraft.com"));
    }

    @Test
    @DisplayName("Cập nhật profile thành công khi giữ nguyên email")
    void updateProfile_Success_SameEmail() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn Mới")
                .email("user@techcraft.com")
                .phone("0999999999")
                .address("999 Cầu Giấy, Hà Nội")
                .build();

        when(userRepository.findByEmail("user@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        UserResponse result = userService.updateProfile("user@techcraft.com", request);

        assertNotNull(result);
        verify(userRepository).save(argThat(u ->
                u.getFullName().equals("Nguyễn Văn Mới") &&
                        u.getPhone().equals("0999999999") &&
                        u.getAddress().equals("999 Cầu Giấy, Hà Nội")
        ));
    }

    @Test
    @DisplayName("Cập nhật profile thành công với email mới chưa tồn tại")
    void updateProfile_Success_NewEmail() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn User")
                .email("newemail@techcraft.com")
                .phone("0987654321")
                .address("123 Phố Huế, Hà Nội")
                .build();

        when(userRepository.findByEmail("user@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("newemail@techcraft.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        UserResponse result = userService.updateProfile("user@techcraft.com", request);

        assertNotNull(result);
        verify(userRepository).save(argThat(u -> u.getEmail().equals("newemail@techcraft.com")));
    }

    @Test
    @DisplayName("Cập nhật profile thất bại khi email mới đã bị người khác sử dụng")
    void updateProfile_DuplicateEmail_ThrowsDuplicateResourceException() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn User")
                .email("occupied@techcraft.com")
                .build();

        when(userRepository.findByEmail("user@techcraft.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("occupied@techcraft.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.updateProfile("user@techcraft.com", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Admin lấy danh sách người dùng phân trang")
    void getUsers_Success() {
        UserFilterRequest filter = UserFilterRequest.builder()
                .page(0)
                .size(10)
                .build();

        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        PageResponse<UserResponse> result = userService.getUsers(filter);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(sampleUser.getEmail(), result.getContent().get(0).getEmail());
    }

    @Test
    @DisplayName("Admin lấy chi tiết user theo ID thành công")
    void getUserById_Success() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        UserResponse result = userService.getUserById(sampleUser.getId());

        assertNotNull(result);
        assertEquals(sampleUser.getId(), result.getId());
    }

    @Test
    @DisplayName("Admin lấy chi tiết user theo ID thất bại khi ID không tồn tại")
    void getUserById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(randomId));
    }

    @Test
    @DisplayName("Admin cập nhật thông tin user thành công")
    void adminUpdateUser_Success() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .fullName("Admin Đã Sửa")
                .role(Role.ADMIN)
                .emailVerified(true)
                .active(false)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(sampleUser)).thenReturn(List.of(
                RefreshToken.builder().id(UUID.randomUUID()).user(sampleUser).revoked(false).build()
        ));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        UserResponse result = userService.adminUpdateUser("admin@techcraft.com", sampleUser.getId(), request);

        assertNotNull(result);
        verify(userRepository).save(argThat(u ->
                u.getFullName().equals("Admin Đã Sửa") &&
                        u.getRole() == Role.ADMIN &&
                        !u.isActive()
        ));
        verify(refreshTokenRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Admin cập nhật email trùng với tài khoản khác sẽ ném ngoại lệ")
    void adminUpdateUser_DuplicateEmail() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .email("other@techcraft.com")
                .build();

        User anotherUser = User.builder().id(UUID.randomUUID()).email("other@techcraft.com").build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.findByEmail("other@techcraft.com")).thenReturn(Optional.of(anotherUser));

        assertThrows(DuplicateResourceException.class, () ->
                userService.adminUpdateUser("admin@techcraft.com", sampleUser.getId(), request));
    }

    @Test
    @DisplayName("Admin không thể tự vô hiệu hóa tài khoản của chính mình")
    void adminUpdateUser_DeactivateSelf_ThrowsBadRequestException() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .active(false)
                .build();

        when(userRepository.findById(sampleAdmin.getId())).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));

        assertThrows(BadRequestException.class, () ->
                userService.adminUpdateUser("admin@techcraft.com", sampleAdmin.getId(), request));
    }

    @Test
    @DisplayName("Admin không thể tự hạ quyền vai trò của chính mình")
    void adminUpdateUser_DemoteSelf_ThrowsBadRequestException() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .role(Role.USER)
                .build();

        when(userRepository.findById(sampleAdmin.getId())).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));

        assertThrows(BadRequestException.class, () ->
                userService.adminUpdateUser("admin@techcraft.com", sampleAdmin.getId(), request));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái vô hiệu hóa tài khoản thành công và thu hồi refresh tokens")
    void updateUserStatus_Deactivate_RevokesRefreshTokens() {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .active(false)
                .build();

        RefreshToken token1 = RefreshToken.builder().id(UUID.randomUUID()).user(sampleUser).revoked(false).build();
        RefreshToken token2 = RefreshToken.builder().id(UUID.randomUUID()).user(sampleUser).revoked(false).build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(sampleUser)).thenReturn(List.of(token1, token2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        UserResponse result = userService.updateUserStatus("admin@techcraft.com", sampleUser.getId(), request);

        assertNotNull(result);
        assertFalse(sampleUser.isActive());
        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());
        verify(refreshTokenRepository).saveAll(List.of(token1, token2));
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Admin không thể tự vô hiệu hóa tài khoản qua endpoint status")
    void updateUserStatus_DeactivateSelf_ThrowsBadRequestException() {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .active(false)
                .build();

        when(userRepository.findById(sampleAdmin.getId())).thenReturn(Optional.of(sampleAdmin));
        when(userRepository.findByEmail("admin@techcraft.com")).thenReturn(Optional.of(sampleAdmin));

        assertThrows(BadRequestException.class, () ->
                userService.updateUserStatus("admin@techcraft.com", sampleAdmin.getId(), request));
        verify(userRepository, never()).save(any(User.class));
    }
}
