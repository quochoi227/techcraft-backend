package com.techcraft.techcraftbackend.service.impl;

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
import com.techcraft.techcraftbackend.service.UserService;
import com.techcraft.techcraftbackend.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "fullName", "email", "role", "active"
    );

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userEmail) {
        User user = findUserByEmail(userEmail);
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(String userEmail, UpdateProfileRequest request) {
        User user = findUserByEmail(userEmail);

        String normalizedNewEmail = request.getEmail().toLowerCase().trim();
        if (!normalizedNewEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(normalizedNewEmail)) {
                throw new DuplicateResourceException("Email đã được sử dụng bởi người dùng khác. Vui lòng chọn email khác.");
            }
            user.setEmail(normalizedNewEmail);
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);
        user.setAddress(request.getAddress() != null && !request.getAddress().isBlank() ? request.getAddress().trim() : null);

        User savedUser = userRepository.save(user);
        log.info("User {} updated profile successfully.", savedUser.getEmail());
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(UserFilterRequest filter) {
        if (filter == null) {
            filter = new UserFilterRequest();
        }

        Pageable pageable = createPageable(filter);
        Specification<User> spec = UserSpecification.filter(
                filter.getEffectiveSearch(),
                filter.getRole(),
                filter.getActive()
        );

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return PageResponse.from(userPage, userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = findUserById(id);
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse adminUpdateUser(String adminEmail, UUID id, AdminUpdateUserRequest request) {
        User targetUser = findUserById(id);
        User currentAdmin = findUserByEmail(adminEmail);

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedNewEmail = request.getEmail().toLowerCase().trim();
            if (!normalizedNewEmail.equalsIgnoreCase(targetUser.getEmail())) {
                userRepository.findByEmail(normalizedNewEmail).ifPresent(existing -> {
                    if (!existing.getId().equals(targetUser.getId())) {
                        throw new DuplicateResourceException("Email đã được sử dụng bởi tài khoản khác.");
                    }
                });
                targetUser.setEmail(normalizedNewEmail);
            }
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            targetUser.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null) {
            targetUser.setPhone(!request.getPhone().isBlank() ? request.getPhone().trim() : null);
        }

        if (request.getAddress() != null) {
            targetUser.setAddress(!request.getAddress().isBlank() ? request.getAddress().trim() : null);
        }

        if (request.getRole() != null) {
            if (currentAdmin.getId().equals(targetUser.getId()) && request.getRole() != Role.ADMIN) {
                throw new BadRequestException("Bạn không thể tự thay đổi vai trò của chính mình.");
            }
            targetUser.setRole(request.getRole());
        }

        if (request.getEmailVerified() != null) {
            targetUser.setEmailVerified(request.getEmailVerified());
        }

        if (request.getActive() != null) {
            if (currentAdmin.getId().equals(targetUser.getId()) && Boolean.FALSE.equals(request.getActive())) {
                throw new BadRequestException("Bạn không thể tự vô hiệu hóa tài khoản của chính mình.");
            }
            targetUser.setActive(request.getActive());
            if (!request.getActive()) {
                revokeUserRefreshTokens(targetUser);
            }
        }

        User savedUser = userRepository.save(targetUser);
        log.info("Admin {} updated user {} successfully.", adminEmail, savedUser.getEmail());
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUserStatus(String adminEmail, UUID id, UpdateUserStatusRequest request) {
        User targetUser = findUserById(id);
        User currentAdmin = findUserByEmail(adminEmail);

        if (currentAdmin.getId().equals(targetUser.getId()) && Boolean.FALSE.equals(request.getActive())) {
            throw new BadRequestException("Bạn không thể tự vô hiệu hóa tài khoản của chính mình.");
        }

        targetUser.setActive(request.getActive());
        if (!request.getActive()) {
            revokeUserRefreshTokens(targetUser);
        }

        User savedUser = userRepository.save(targetUser);
        log.info("Admin {} changed status of user {} to active={}.", adminEmail, savedUser.getEmail(), savedUser.isActive());
        return userMapper.toUserResponse(savedUser);
    }

    private User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email người dùng không được để trống.");
        }
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    private User findUserById(UUID id) {
        if (id == null) {
            throw new BadRequestException("ID người dùng không được để trống.");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    private void revokeUserRefreshTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        if (!activeTokens.isEmpty()) {
            activeTokens.forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
            log.info("Revoked {} active refresh tokens for deactivated user {}.", activeTokens.size(), user.getEmail());
        }
    }

    private Pageable createPageable(UserFilterRequest filter) {
        String sortBy = filter.getSortBy();
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(direction, sortBy));
    }
}
